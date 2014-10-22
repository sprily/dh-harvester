package uk.co.sprily.dh
package harvester

import akka.actor.ActorContext
import akka.actor.ActorSelection

import org.joda.{time => joda}

import network.Device

trait DeviceActorDirectoryService[D <: Device] {
  val Protocol: DeviceActorProtocol[D]

  def lookup(device: D)(implicit context: ActorContext): ActorSelection
}

trait DeviceActorProtocol[D <: Device] {
  case class Poll(d: D, selection: D#AddressSelection)
  case class PollResult(timestamp: joda.LocalDateTime)
}
