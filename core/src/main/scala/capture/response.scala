package uk.co.sprily.dh
package harvester
package capture

import org.joda.time.LocalDateTime

import network.Device

trait Response {
  type D <: Device
  type Measurement = D#Measurement

  val timestamp: LocalDateTime
  val device: D
  val measurement: Measurement
}
