package uk.co.sprily
package dh
package harvester

case class DeviceId(v: Long) extends AnyVal

trait Device {

  type Address
  type RegisterSelection

  def id: DeviceId
  def address: Address
}
