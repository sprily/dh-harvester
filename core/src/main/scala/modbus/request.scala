package uk.co.sprily.dh
package harvester
package modbus

import capture.RequestLike

case class ModbusRequest(
    id: Long,
    device: ModbusDevice,
    selection: ModbusRegisterRange) extends RequestLike {

  type Device = ModbusDevice

}
