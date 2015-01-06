package uk.co.sprily.dh
package harvester
package api

import scala.concurrent.duration._

import capture.Request
import modbus.ModbusDevice
import modbus.ModbusDeviceAddress
import modbus.ModbusRegisterRange
import modbus.ModbusRequest
import network.Device
import network.DeviceId
import network.IP4Address
import network.TCPGateway
import scheduling.Schedule

case class ManagedInstance(devices: Seq[ManagedDevice])

trait ManagedDevice {

  // The type of Request this DTO can generate
  type R <: Request

  // How that Request's DTO is modelled
  protected type RequestDTO

  // Given a RequestDTO instance, return the Request
  protected def toRequest(rDTO: RequestDTO): R
  protected def toSchedule(rDTO: RequestDTO): Schedule
  protected def requestDTOs: Seq[RequestDTO]
  private def requests: Seq[R] = requestDTOs.map(toRequest(_))
  private def schedules: Seq[Schedule] = requestDTOs.map(toSchedule(_))

  def device: R#D
  def scheduledRequests: Seq[(R,Schedule)] = requests.zip(schedules)
}

case class ManagedModbusDevice(
    id: Long,
    unitId: Int,
    gateway: TCPGatewayDTO,
    requestDTOs: Seq[ModbusRequestDTO]) extends ManagedDevice {

  type R = ModbusRequest
  type RequestDTO = ModbusRequestDTO

  override def toRequest(r: RequestDTO) = ModbusRequest(
    id = id,
    device = device,
    selection = ModbusRegisterRange(r.range._1, r.range._2))

  override def toSchedule(r: RequestDTO) = {
    Schedule.each(r.interval.seconds).fixTimeoutTo(10.seconds)
  }

  def device = ModbusDevice(
    DeviceId(id),
    ModbusDeviceAddress(
      unitId.toByte,  // TODO
      gateway.gateway)
  )

}

case class ModbusRequestDTO(id: Long, range: (Int, Int), interval: Int)

case class TCPGatewayDTO(
    host: String,
    port: Int) {

  def gateway = TCPGateway(
    IP4Address.fromString(host).get,
    port)

}
