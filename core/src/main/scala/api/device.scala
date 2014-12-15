package uk.co.sprily.dh
package harvester
package api

import network.Device
import network.DeviceId
import network.TCPGateway
import network.IP4Address
import modbus.ModbusDevice
import modbus.ModbusDeviceAddress

sealed trait DeviceDTO[D <: Device] {
  def device: D   // TODO: Option[D]
}

case class ModbusDeviceDTO(
    id: Long,
    unitId: Int,
    gatewayDTO: TCPGatewayDTO) extends DeviceDTO[ModbusDevice] {

  def device = ModbusDevice(
    DeviceId(id),
    ModbusDeviceAddress(
      unitId.toByte,    // TODO
      gatewayDTO.gateway)
  )

}

case class TCPGatewayDTO(
    host: String,
    port: Int) {

  def gateway = TCPGateway(
    IP4Address.fromString(host).get,
    port)

}
