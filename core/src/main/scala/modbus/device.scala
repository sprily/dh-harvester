package uk.co.sprily.dh
package harvester
package modbus

import harvester.network.Device
import harvester.network.DeviceId
import harvester.network.TCPGateway

case class ModbusDeviceAddress(
    val deviceNumber: Byte,
    val gateway: TCPGateway)

case class ModbusRegisterRange(
    val startRegister: Int,
    val endRegister: Int)

case class ModbusDevice(
    val id: DeviceId,
    val address: ModbusDeviceAddress) extends Device {

  type Address = ModbusDeviceAddress
  type AddressSelection = ModbusRegisterRange
  type Measurement = String
}

