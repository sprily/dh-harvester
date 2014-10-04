package uk.co.sprily.dh
package harvester

import akka.actor.ActorContext
import akka.actor.ActorSelection

import org.joda.{time => joda}

import network.Device

trait DeviceDirectoryService[D <: Device] {
  type Protocol <: DeviceActorProtocol[D]
  val Protocol: Protocol

  def lookup(device: D)(implicit context: ActorContext): ActorSelection
}

trait DeviceActorProtocol[D <: Device] {
  case class Poll(d: D, selection: D#AddressSelection)
  case class PollResult(timestamp: joda.LocalDateTime)
}
