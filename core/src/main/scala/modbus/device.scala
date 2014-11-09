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
  type Measurement = ModbusMeasurement
}

/** The result of reading registers from a modbus device.
  *
  * Modbus defines a register value to be a 16-bit wide word.
  * Some devices use this to encode an unsigned short, others
  * a signed short, some even use it to encode bit fields.  So
  * treat the values as words, and interpret their meaning higher
  * up the chain where we know the make and model of the device.
  */
case class ModbusMeasurement(
    val range: ModbusRegisterRange,
    val words: Seq[Word16])

object ModbusMeasurement {
  implicit val serialiser: Serialiser[ModbusMeasurement] = new Serialiser[ModbusMeasurement] {
    override def toBytes(m: ModbusMeasurement) = {
      List()
    }
  }
}

case class Word16(s: Short) extends AnyRef
