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

import akka.actor.{ActorSystem, Props}

object Application {
  def main(args: Array[String]) {
    val system = ActorSystem("dhcp-sim")
    println("Application is running")

    // Make a server
    val server = system.actorOf(Props[Server], "DHCP-server")

    // Broadcast helper
    val broadcastAgent = system.actorOf(Props[BroadcastAgent], "DHCP-broadcast-agent")

    // Make a bunch of clients
    val clients = (1 to 50).sortBy(_ % 3).toList.map{ n => system.actorOf(Props[Client], "DHCP-client-" + n) }
  }
}
