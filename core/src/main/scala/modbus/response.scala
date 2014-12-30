package uk.co.sprily.dh
package harvester
package modbus

import org.joda.time.LocalDateTime
import org.joda.time.DateTimeZone

import capture.Response

case class ModbusResponse(
    device: ModbusDevice,
    measurement: ModbusMeasurement,
    timestamp: LocalDateTime) extends Response {
  type D = ModbusDevice
}

object ModbusResponse {
  def apply(d: ModbusDevice, m: ModbusMeasurement): ModbusResponse = {
    ModbusResponse(d, m, LocalDateTime.now(DateTimeZone.UTC))
  }
}
