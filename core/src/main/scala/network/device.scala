package uk.co.sprily.dh
package harvester
package network

case class DeviceId(v: Long) extends AnyVal

trait Device {

  /**
    * Each device type has its own way of addressing registers or sensors.
    */
  type Address

  /**
    * Similarly to `Address`, each device will have a particular way of
    * identifying a selection of `Address`es.
    */
  type AddressSelection

  def id: DeviceId
  def address: Address
}
