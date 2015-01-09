package uk.co.sprily.dh
package harvester
package api

import scala.util.Try

import network.DeviceId
import network.DeviceLike
import network.IP4Address
import network.TCPGateway
import modbus.ModbusDevice
import modbus.ModbusDeviceAddress

case class ManagedDevices(devices: Seq[ManagedDevice])

trait ManagedDevice {
  type Device <: DeviceLike
  def device: Try[Device]
}

case class ManagedModbusDevice(
    id: Long,
    unitId: Int,
    host: String,
    port: Int) extends ManagedDevice {

  type Device = ModbusDevice

  def device = for {
    host <- IP4Address.tryFromString(host)
    gateway = TCPGateway(host, port)
    address = ModbusDeviceAddress(unitId.toByte, gateway)
    device = ModbusDevice(DeviceId(id), address)
  } yield device

}

