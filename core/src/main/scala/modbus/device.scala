package uk.co.sprily.dh
package harvester
package modbus

import akka.util.ByteString

import scodec._
import codecs._

import harvester.network.DeviceId
import harvester.network.DeviceLike
import harvester.network.TCPGateway

import protocols.codecs

case class ModbusDeviceAddress(
    val deviceNumber: Byte,
    val gateway: TCPGateway)

/** Inclusive range **/
case class ModbusRegisterRange(
    val startRegister: Int,
    val endRegister: Int) {

  assert (startRegister <= endRegister)

  val numRegisters = endRegister - startRegister + 1

}

case class ModbusDevice(
    val id: DeviceId,
    val address: ModbusDeviceAddress) extends DeviceLike {

  type Address = ModbusDeviceAddress
  type AddressSelection = ModbusRegisterRange
  type Measurement = ModbusMeasurement
}

/** The result of reading registers from a modbus device.
  *
  * Modbus defines a register value to be a 16-bit wide word.
  * Some devices use this to encode an unsigned short, others
  * a signed short, some even use it to encode bit fields.  Rather
  * than interpreting their meaning, we just store the raw bytes
  * as an immutable ByteString, and  interpret their meaning higher
  * up the chain where we know the make and model of the device.
  */
case class ModbusMeasurement(
    val range: ModbusRegisterRange,
    val values: ByteString)

object ModbusMeasurement {

  implicit val codec: Codec[ModbusMeasurement] = {
    val rangeCodec = (uint16 :: uint16).as[ModbusRegisterRange]
    val valuesCodec = variableSizeBits(uint8, codecs.byteString)
    (rangeCodec :: valuesCodec).as[ModbusMeasurement]
  }

}
