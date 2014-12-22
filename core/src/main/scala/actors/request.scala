package uk.co.sprily.dh
package harvester
package capture

import org.joda.time.LocalDateTime

import network.Device

import scheduling.Schedule
import scheduling.TargetLike

trait Request2 {
  type D <: Device
  type Selection = D#AddressSelection

  val device: D
  val selection: Selection
}

trait Response2 {
  type D <: Device
  type Measurement = D#Measurement

  val timestamp: LocalDateTime
  val device: D
  val measurement: Measurement
}
