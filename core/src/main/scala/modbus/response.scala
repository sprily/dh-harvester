package uk.co.sprily.dh
package harvester
package modbus

import org.joda.time.LocalDateTime
import org.joda.time.DateTimeZone

import capture.ResponseLike

case class ModbusResponse(
    device: ModbusDevice,
    measurement: ModbusMeasurement,
    timestamp: LocalDateTime) extends ResponseLike {
  type Device = ModbusDevice
}

object ModbusResponse {
  def apply(d: ModbusDevice, m: ModbusMeasurement): ModbusResponse = {
    ModbusResponse(d, m, LocalDateTime.now(DateTimeZone.UTC))
  }
}
