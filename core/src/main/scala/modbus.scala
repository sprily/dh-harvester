package uk.co.sprily
package dh
package harvester

case class ModbusDeviceAddress(
    val deviceNumber: Byte,
    val gateway: TCPGateway)

case class ModbusRegisterRange(
    val startRegister: Byte,
    val endRegister: Byte)

case class ModbusDevice(
    val id: DeviceId,
    val address: ModbusDeviceAddress) extends Device {

  type Address = ModbusDeviceAddress
  type RegisterSelection = ModbusRegisterRange
}
