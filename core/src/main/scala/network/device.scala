package uk.co.sprily.dh
package harvester
package network

import org.joda.time.LocalDateTime

case class DeviceId(v: Long) extends AnyVal

trait Device { self =>

  /**
    * Each device type has its own way of addressing registers or sensors.
    */
  type Address

  /**
    * Similarly to `Address`, each device will have a particular way of
    * identifying a selection of `Address`es.
    */
  type AddressSelection

  /**
    * Finally, each Device produces its own `Measurement` type.
    */
  type Measurement

  case class Reading(
      val timestamp: LocalDateTime,
      val device: Device,
      val measurement: Device#Measurement)

  object Reading {
    def apply(t: LocalDateTime, m: Device#Measurement): Reading = Reading(t, self, m)
  }

  def id: DeviceId
  def address: Address
}
