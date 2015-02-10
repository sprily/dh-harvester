package uk.co.sprily.dh
package harvester
package capture

import network.DeviceLike

trait RequestLike {
  type Device <: DeviceLike
  type Selection = Device#AddressSelection

  val device: Device
  val selection: Selection
}
