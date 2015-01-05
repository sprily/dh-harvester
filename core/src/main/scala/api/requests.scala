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

// QUESTION: can we turn this into a type-class?

trait ManagedDevice {

  // The type of Request this DTO can generate
  type R <: Request

  // How that Request's DTO is modelled
  protected type RequestDTO

  // Given a RequestDTO instance, return the Request
  protected def toRequest(rDTO: RequestDTO): R
  protected val requestDTOs: Seq[RequestDTO]

  def device: R#D
  def requests: Seq[R] = requestDTOs.map(toRequest(_))
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

  def device = ModbusDevice(
    DeviceId(id),
    ModbusDeviceAddress(
      unitId.toByte,  // TODO
      gateway.gateway)
  )

}

case class ModbusRequestDTO(id: Long, range: (Int, Int))

case class TCPGatewayDTO(
    host: String,
    port: Int) {

  def gateway = TCPGateway(
    IP4Address.fromString(host).get,
    port)

}
