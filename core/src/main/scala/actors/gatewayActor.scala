package uk.co.sprily.dh
package harvester
package capture

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props

import network.TCPGateway

class GatewayActorDirectory extends ActorDirectory {

  type Key = TCPGateway
  val protocol = GatewayActorDirectory.Protocol

  override def actorPath(gw: TCPGateway) = {
    List(gw.address, gw.port.toString).mkString(":")
  }
}

object GatewayActorDirectory {
  object Protocol extends ActorDirectory.Protocol[TCPGateway]
  def name = "gw-actor-directory"
}

