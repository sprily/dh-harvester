package uk.co.sprily.dh
package harvester
package network

case class DeviceId(v: Long) extends AnyVal

trait Device {

  type Address
  type RegisterSelection

  def id: DeviceId
  def address: Address
}
