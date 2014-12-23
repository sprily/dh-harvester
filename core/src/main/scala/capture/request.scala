package uk.co.sprily.dh
package harvester
package capture

import network.Device
import scheduling.Schedule
import scheduling.TargetLike

trait Request {
  type D <: Device
  type Selection = D#AddressSelection

  val device: D
  val selection: Selection
}

