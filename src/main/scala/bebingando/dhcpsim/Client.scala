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

import java.time.Instant

import akka.actor.{Actor, ActorRef}

class Client extends Actor {

  val clientHardwareAddress = MacGenerator.getMac
  var serverAddress: Option[String] = None
  var ipAddress: Option[String] = None
  var leaseExpirationTime: Option[Long] = None
  var leaseRenewTime: Option[Long] = None
  var leaseRebindTime: Option[Long] = None

  val releaseTime: Long = Instant.now().toEpochMilli() + 60000

  case class Timer()

  private val server = context.system.actorSelection("/user/DHCP-server")
  private val broadcastAgent = context.system.actorSelection("/user/DHCP-broadcast-agent")

  override def preStart() = {
    println("DHCP Client coming on line for " + clientHardwareAddress)

    val sleepMs = Math.random() * 5000
    Thread.sleep(sleepMs.round)
    broadcastAgent ! DHCPDISCOVER(clientHardwareAddress, DHCPMessageType.DISCOVER)
  }

  def receive = {
    case o: DHCPOFFER => handleDHCPOffer(o, sender())
    case a: DHCPACK => handleDHCPAck(a, sender())
    case t: String => prepareForRenewal()
  }

  private def handleDHCPOffer(offer: DHCPOFFER, sender: ActorRef) = {
    if (offer.chaddr == clientHardwareAddress) {
      println("Client received " + offer + " from " + sender)
      broadcastAgent ! DHCPREQUEST(offer.siaddr, clientHardwareAddress, None, Some(offer.yiaddr), Some(offer.siaddr), DHCPMessageType.REQUEST)
    }
  }

  private def handleDHCPAck(ack: DHCPACK, sender: ActorRef) = {
    if (ack.chaddr == clientHardwareAddress) {
      println("Client received " + ack + " from " + sender)
      this.serverAddress = Some(ack.siaddr)
      this.ipAddress = Some(ack.yiaddr)
      this.leaseExpirationTime = Some(Instant.now().toEpochMilli() + ack.leaseTime)
      this.leaseRenewTime = Some((Instant.now().toEpochMilli() + .5*ack.leaseTime).round)
      this.leaseRebindTime = Some((Instant.now().toEpochMilli() + .875*ack.leaseTime).round)
      self ! "timer"
    }
  }

  private def prepareForRenewal() = {
    println("Client " + clientHardwareAddress + ipAddress.map(a => " (" + a + ")").getOrElse("") + " awaiting renewal time")
    while (Instant.now().toEpochMilli() < leaseRenewTime.getOrElse(60000L)) {
      Thread.sleep(1000)
    }
    for {
      sa <- serverAddress
      ip <- ipAddress
    } yield {
      if (Instant.now().toEpochMilli() < releaseTime) {
        server ! DHCPREQUEST(sa, clientHardwareAddress, Some(ip), None, None, DHCPMessageType.REQUEST)
      } else {
        server ! DHCPRELEASE(ip, clientHardwareAddress, DHCPMessageType.RELEASE)
      }
    }
  }
}
