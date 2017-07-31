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

object DHCPMessageType extends Enumeration {
  type DHCPMessageType = Value
  val DISCOVER, OFFER, REQUEST, ACK, NAK, RELEASE = Value
}

object LeaseStatus extends Enumeration {
  type LeaseStatus = Value
  val HELD, RESERVED, RELEASED = Value
}

case class AddressStatus(address: String, status: LeaseStatus.Value, expiration: Option[Long])

abstract case class DHCPMessage(messageType: DHCPMessageType.Value)

case class DHCPDISCOVER(
  chaddr: String,
  messageType: DHCPMessageType.Value
)

case class DHCPOFFER(
  yiaddr: String,
  siaddr: String,
  chaddr: String,
  leaseTime: Int,
  messageType: DHCPMessageType.Value  
)

case class DHCPREQUEST(
  siaddr: String,
  chaddr: String,
  ciaddr: Option[String],
  requestedAddr: Option[String],
  serverAddr: Option[String],
  messageType: DHCPMessageType.Value
)

case class DHCPACK(
  yiaddr: String,
  siaddr: String,
  chaddr: String,
  ciaddr: Option[String],
  leaseTime: Long,
  messageType: DHCPMessageType.Value
)

case class DHCPRELEASE(
  yiaddr: String,
  chaddr: String,
  messageType: DHCPMessageType.Value
)
