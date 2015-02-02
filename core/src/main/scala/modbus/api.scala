package uk.co.sprily.dh
package harvester
package modbus

import scala.concurrent.duration._

import controllers.InstanceManager
import network.DeviceId
import network.DeviceLike
import network.IP4Address
import network.TCPGateway
import scheduling.Schedule

import InstanceManager.Protocol.ManagedDevice

case class ModbusManagedDevice(
    id: Long,
    unitId: Int,
    host: String,
    port: Int,
    reqs: Seq[ModbusManagedRequest]) extends ManagedDevice {

  type Request = ModbusRequest
  type Device = ModbusDevice

  override def device = ModbusDevice(DeviceId(id), address)
  override def requests = reqs.map { r => (r.request(device), r.schedule) }

  private[this] def address = ModbusDeviceAddress(unitId.toByte, gateway)
  private[this] def gateway = TCPGateway(IP4Address.fromString(host).get, port)
}

case class ModbusManagedRequest(
    id: Long,
    interval: Int,
    fromAddress: Int,
    toAddress: Int) {

  def request(d: ModbusDevice) = ModbusRequest(
    id = id,
    device = d,
    selection = ModbusRegisterRange(fromAddress, toAddress))

  def schedule = Schedule.each(interval.seconds).fixTimeoutTo(10.seconds)

}
