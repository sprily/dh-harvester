package uk.co.sprily.dh
package harvester

import akka.actor.ActorContext
import akka.actor.ActorSelection

import org.joda.time.LocalDateTime

import network.Device

trait DeviceActorDirectoryService[D <: Device] {

  object Protocol {
    case class Poll(d: D, selection: D#AddressSelection)
    case class Result(timestamp: LocalDateTime, measurement: D#Measurement)
  }

  def lookup(device: D): ActorSelection
}
