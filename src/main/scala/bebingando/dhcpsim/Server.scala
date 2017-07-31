/**
 * Copyright 2017 Steve Black
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bebingando.dhcpsim

import java.net.InetAddress
import java.time.Instant

import scala.collection.mutable.HashMap

import akka.actor.{Actor, ActorRef}

class Server extends Actor {
  override def preStart() = {
    println("DHCP Server coming on line at " + ServerAddress)
  }

  private val ServerAddress = "192.168.1.1"
  private val LeaseDuration = 10 * 1000 // seconds; closer to 86400 in reality (24 hrs)
  private val broadcastAgent = context.system.actorSelection("/user/DHCP-broadcast-agent")
  private val leases: HashMap[String,AddressStatus] = new HashMap[String,AddressStatus]() /** MAC -> IP */

  // 192.168.1.{0,1,255} are special -- allocate the rest freely!
  // range from 2 to 254 available (allocate [randomly?] within that)

  private val RangeCount = 253

  private def generateIp(): Option[String] = {
    (RangeCount - leases.filterNot( { case (k,v) => v.status == LeaseStatus.RESERVED } ).size) match {
      case n if (n > 0) => {
        val rnd = new scala.util.Random
        val assigned = 2 + rnd.nextInt(RangeCount) // (254 - 2) + 1
        println("Reserving 192.168.1." + assigned.toString)
        Some("192.168.1." + assigned.toString)
      }
      case _ => { 
        println("Insufficient addresses available!")
        None
      }
    }
  }

  def receive = {
    case d: DHCPDISCOVER => handleDHCPDiscover(d, sender())
    case rq: DHCPREQUEST => handleDHCPRequest(rq, sender())
    case rl: DHCPRELEASE => handleDHCPRelease(rl, sender())
  }

  private def handleDHCPDiscover(discover: DHCPDISCOVER, sender: ActorRef) = {
    println("Server received " + discover + " from " + sender)
    val mac = discover.chaddr
    leases.get(mac) match {
      case Some(as) if (as.status == LeaseStatus.RELEASED) => {
        leases.update(mac, AddressStatus(as.address, LeaseStatus.HELD, None))
        broadcastAgent ! DHCPOFFER(as.address, ServerAddress, mac, LeaseDuration, DHCPMessageType.OFFER)
      }
      case Some(as) => println(mac + " found in lease map!") // FIXME: what to do here?
      case None => generateIp().foreach{ ip => 
        leases += ((mac, AddressStatus(ip, LeaseStatus.HELD, None)))
        broadcastAgent ! DHCPOFFER(ip, ServerAddress, mac, LeaseDuration, DHCPMessageType.OFFER)
      }
    }
  }

  private def handleDHCPRequest(request: DHCPREQUEST, sender: ActorRef) = {
    println("Server received " + request + " from " + sender)
    val mac = request.chaddr
    leases.get(mac) match {
      case Some(as) if (as.status == LeaseStatus.HELD && request.serverAddr.isDefined) => {
        /** Initial Request */ 
        leases.update(mac, AddressStatus(as.address, LeaseStatus.RESERVED, Some(Instant.now().toEpochMilli() + LeaseDuration)))
        println("Successuflly leased to " + mac + " address " + as.address)
        broadcastAgent ! DHCPACK(as.address, ServerAddress, mac, None, LeaseDuration, DHCPMessageType.ACK) /** Broadcast */
      }
      case Some(as) if (as.status == LeaseStatus.RESERVED && !request.serverAddr.isDefined && !request.requestedAddr.isDefined && request.ciaddr.isDefined) => {
        /** Renewal Request */
        leases.update(mac, AddressStatus(as.address, LeaseStatus.RESERVED, Some(Instant.now().toEpochMilli() + LeaseDuration)))
        println("Successuflly renewed " + mac + "'s lease on " + as.address)
        sender ! DHCPACK(as.address, ServerAddress, mac, request.ciaddr, LeaseDuration, DHCPMessageType.ACK) /** Unicast direct to client */
      }
      case _ => println("DHCPRequest: Client didn't comply with protocol")
    }
  }

  private def handleDHCPRelease(release: DHCPRELEASE, sender: ActorRef) = {
    println("Server received " + release + " from " + sender)
    val mac = release.chaddr
    leases.get(mac) match {
      case Some(as) if (as.status == LeaseStatus.RESERVED && as.address == release.yiaddr) => {
        leases.update(mac, AddressStatus(as.address, LeaseStatus.RELEASED, None))
      }
      case _ => println("DHCP Release: Client didn't comply with protocol")
    }
  }
}
