package uk.co.sprily.dh
package harvester
package modbus

import capture.Request

case class ModbusRequest(
    device: ModbusDevice,
    selection: ModbusRegisterRange) extends Request {

  type D = ModbusDevice

}
