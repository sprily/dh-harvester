package uk.co.sprily.dh
package harvester
package capture

import network.DeviceLike
import scheduling.Schedule
import scheduling.TargetLike

trait RequestLike {
  type Device <: DeviceLike
  type Selection = Device#AddressSelection

  val id: Long
  val device: Device
  val selection: Selection
}

