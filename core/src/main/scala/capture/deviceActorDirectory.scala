package uk.co.sprily.dh
package harvester
package capture

import akka.actor.Props

import actors.ActorDirectory
import network.DeviceLike

/** Directory of DeviceActors
  *
  * Lookup a `Device`'s associated `ActorRef`, or send it a message
  * directory using `Forward`.
  */
class DeviceActorDirectory extends ActorDirectory {
  type Key = DeviceLike
  val protocol = DeviceActorDirectory.Protocol
  override def actorPath(d: DeviceLike) = d.id.v.toString
}

object DeviceActorDirectory {
  object Protocol extends ActorDirectory.Protocol[DeviceLike]
  def name = "device-actor-directory"
  def props = Props(new DeviceActorDirectory())
}
