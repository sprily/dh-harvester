package uk.co.sprily.dh
package harvester

import akka.actor.ActorContext
import akka.actor.ActorSelection

import org.joda.{time => joda}

import network.Device

trait GatewayService[D <: Device] {
  object Protocol extends GatewayProtocol[D]
  def lookup(device: D)(implicit context: ActorContext): ActorSelection
}

trait GatewayProtocol[D <: Device] {
  case class Poll(d: D, selection: D#AddressSelection)
  case class PollResult(timestamp: joda.LocalDateTime)
}
