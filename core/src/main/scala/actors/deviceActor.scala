package uk.co.sprily.dh
package harvester
package capture

import network.Device

class DeviceActorDirectory2 extends ActorDirectory {
  type Key = Device
  val protocol = DeviceActorDirectory2.Protocol
  override def actorPath(d: Device) = d.id.v.toString
}

object DeviceActorDirectory2 {
  object Protocol extends ActorDirectory.Protocol[Device]
  def name = "device-actor-directory"
}
