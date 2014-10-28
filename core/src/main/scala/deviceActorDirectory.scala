package uk.co.sprily.dh
package harvester

import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorLogging
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props

import org.joda.time.LocalDateTime

import network.Device

trait DeviceActorDirectory[D <: Device] {

  object Protocol {
    case class Poll(d: D, selection: D#AddressSelection)
    case class Result(timestamp: LocalDateTime, measurement: D#Measurement)
  }

  def lookup(device: D): ActorSelection
}
