package uk.co.sprily.dh
package harvester
package api

import scala.concurrent.duration._

import capture.RequestLike
import modbus.ModbusDevice
import modbus.ModbusDeviceAddress
import modbus.ModbusRegisterRange
import modbus.ModbusRequest
import network.DeviceId
import network.IP4Address
import network.TCPGateway
import scheduling.Schedule

case class ManagedRequests(requests: Seq[DeviceRequests])

trait DeviceRequests {

  // The type of Request this DTO can generate
  type Request <: RequestLike

  def deviceId: DeviceId
  def schedule: Schedule
  def requests(d: Request#Device): Seq[Request]
}

case class ModbusDeviceRequests(
    id: Long,
    interval: Int,
    requestDTOs: Seq[ModbusRequestDTO]) extends DeviceRequests {

  type Request = ModbusRequest

  def deviceId = DeviceId(id)
  def schedule = Schedule.each(interval.seconds)fixTimeoutTo(10.seconds)
  def requests(d: Request#Device) = requestDTOs.map(_.toRequest(d, schedule))
}

case class ModbusRequestDTO(
    id: Long,
    fromAddress: Int,
    toAddress: Int) {

  def toRequest(d: ModbusDevice, s: Schedule) = ModbusRequest(
    id = id,
    device = d,
    selection = ModbusRegisterRange(fromAddress, toAddress))

}

case class TCPGatewayDTO(
    host: String,
    port: Int) {

  def gateway = TCPGateway(
    IP4Address.fromString(host).get,
    port)

}
