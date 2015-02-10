package uk.co.sprily.dh
package harvester
package modbus

import akka.util.ByteString

import scalaz._
import Scalaz._

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

object ModbusRegisterRange {

  def validated(startRegister: Int, endRegister: Int) = {
    withinBounds(startRegister)                 *>
    withinBounds(endRegister)                   *>
    nonVacuousRange(startRegister, endRegister).map((ModbusRegisterRange.apply _).tupled)
  }

  private def withinBounds(reg: Int) = reg match {
    case reg if reg < 0     => "Negative address".failureNel
    case reg if reg > 65536 => "Address > 65536".failureNel
    case reg                => reg.success
  }

  private def nonVacuousRange(start: Int, end: Int) = (start, end) match {
    case (start, end) if start > end => "Empty address range".failureNel
    case (start, end)                => (start, end).success
  }

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
