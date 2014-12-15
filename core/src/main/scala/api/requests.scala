package uk.co.sprily.dh
package harvester
package api

import scala.concurrent.duration._

import capture.DeviceManagerActor
import capture.Request
import modbus.ModbusDevice
import modbus.ModbusDeviceAddress
import modbus.ModbusRegisterRange
import network.Device
import scheduling.Schedule

case class Management(requests: Seq[PersistentRequestDTO[_]])

case class PersistentRequestDTO[D <: Device](
    deviceDTO: DeviceDTO[D],
    requestsDTO: Seq[RequestDTO[D]]) {

  import DeviceManagerActor.Protocol._

  def device: D = deviceDTO.device
  def requests: Seq[Request[D]] = {
    requestsDTO.map(_.request(device))
  }

  def dRequests: Seq[DeviceRequest] = requestsDTO.map {
    case (r: ModbusRequestDTO) => ModbusRequest(r.request(device.asInstanceOf[ModbusDevice]))
  }

}

sealed trait RequestDTO[D <: Device] {
  val id: Long
  val interval: Long

  def request: D => Request[D]
}

case class ModbusRequestDTO(
    id: Long,
    interval: Long,
    registers: (Int, Int)) extends RequestDTO[ModbusDevice] {

  def request = Request[ModbusDevice](
    id,
    Schedule.each(interval.millis),
    _: ModbusDevice,
    ModbusRegisterRange(registers._1, registers._2)
  )

}
