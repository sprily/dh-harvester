package uk.co.sprily
package dh
package harvester

import akka.actor.ActorContext
import akka.actor.ActorSelection

import org.joda.{time => joda}

trait GatewayService[D <: Device] {
  object Protocol extends GatewayProtocol[D]
  def lookup(device: D)(implicit context: ActorContext): ActorSelection
}

trait GatewayProtocol[D <: Device] {
  case class Poll(d: D, selection: D#RegisterSelection)
  case class PollResult(timestamp: joda.LocalDateTime)
}
