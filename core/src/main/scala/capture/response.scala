package uk.co.sprily.dh
package harvester
package capture

import org.joda.time.LocalDateTime

import network.DeviceLike

trait ResponseLike {
  type Device <: DeviceLike
  type Measurement = Device#Measurement

  val timestamp: LocalDateTime
  val device: Device
  val measurement: Measurement
}
