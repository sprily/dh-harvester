package uk.co.sprily.dh
package harvester
package network

import java.net._
import java.io._

import com.ghgande.j2mod.modbus._
import com.ghgande.j2mod.modbus.msg._
import com.ghgande.j2mod.modbus.io._
import com.ghgande.j2mod.modbus.net._
import com.ghgande.j2mod.modbus.util._


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

object ModbusTest {
  def main(args: Array[String]): Unit = {
    val conn = new TCPMasterConnection(InetAddress.getByName("localhost"))
    conn.setPort(5020)
    conn.connect()

    val req = new ReadMultipleRegistersRequest(50520, 4)
    req.setUnitID(1)
    val tx = new ModbusTCPTransaction(conn)
    tx.setRequest(req)

    (1 to 10).foreach { _ =>
      tx.execute()
      val res = tx.getResponse().asInstanceOf[ReadMultipleRegistersResponse]

      val results = res.getRegisters.map(_.toUnsignedShort)
      results.foreach(println)
      println
      Thread.sleep(1000)
    }


    conn.close()
  }
}
