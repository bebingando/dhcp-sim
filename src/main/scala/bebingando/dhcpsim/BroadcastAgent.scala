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

import akka.actor.{Actor, ActorRef}

class BroadcastAgent extends Actor {

  private val serverId = "/user/DHCP-server"
  private val clientPath = "/user/DHCP-client*"

  def receive = {
    /** Look on client/server for unicast message handling */

    /** Client -> Server */
    case d: DHCPDISCOVER => handleDHCPDiscover(d, sender())
    case r: DHCPREQUEST => handleDHCPRequest(r, sender())

    /** Server -> Client */
    case o: DHCPOFFER => handleDHCPOffer(o, sender())
    case a: DHCPACK => handleDHCPAck(a, sender())
  }

  private def handleDHCPDiscover(discover: DHCPDISCOVER, sender: ActorRef) = {
    println("Broadcast " + discover + " from sender " + sender)
    val server = context.system.actorSelection(serverId)
    server.tell(discover, sender)
  }

  private def handleDHCPRequest(request: DHCPREQUEST, sender: ActorRef) = {
    println("Broadcast  " + request + " from sender " + sender)
    val server = context.system.actorSelection(serverId)
    server.tell(request, sender)
  }

  private def handleDHCPOffer(offer: DHCPOFFER, sender: ActorRef) = {
    println("Broadcast " + offer + " from sender " + sender)
    val clients = context.system.actorSelection(clientPath)
    clients.tell(offer, sender)
  }

  private def handleDHCPAck(ack: DHCPACK, sender: ActorRef) = {
    println("Broadcast " + ack + " from sender " + sender)
    val clients = context.system.actorSelection(clientPath)
    clients.tell(ack, sender)
  }
}
