package uk.co.sprily.dh
package harvester
package modbus

import java.net._
import java.io._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.routing.RoundRobinPool

import com.ghgande.j2mod.modbus._
import com.ghgande.j2mod.modbus.msg._
import com.ghgande.j2mod.modbus.io._
import com.ghgande.j2mod.modbus.net._
import com.ghgande.j2mod.modbus.util._

import org.joda.time.LocalDateTime

import harvester.network.TCPGateway


object ConnectionActor {

  def gateway(gateway: TCPGateway,
              directory: DeviceActorDirectoryService[ModbusDevice],
              minConnections: Int = 1,
              maxConnections: Int = 4): Props = {
    props(gateway, directory).
      withRouter(
        RoundRobinPool(
          nrOfInstances=minConnections,
          resizer=None
        )
      )
  }

  private def props(gateway: TCPGateway,
            directory: DeviceActorDirectoryService[ModbusDevice]): Props = {
    Props(new ConnectionActor(gateway, directory))
  }
}

/** Manages a single connection to a modbus TCP gateway **/
class ConnectionActor(
    val gateway: TCPGateway,
    directory: DeviceActorDirectoryService[ModbusDevice]) extends Actor
                                                             with ActorLogging {

  import directory.Protocol._

  val conn = new TCPMasterConnection(gateway.address.inet)
  conn.setPort(gateway.port)
  conn.connect()

  def receive = {
    case p@Poll(d: ModbusDevice, registers: ModbusRegisterRange) =>
      pollRcvd(p)
  }

  private def pollRcvd(p: Poll): Unit = {
    val req = new ReadMultipleRegistersRequest(
      p.selection.startRegister,
      p.selection.endRegister)
    req.setUnitID(p.d.address.deviceNumber)

    val tx = new ModbusTCPTransaction(conn)
    tx.setRequest(req)
    
    tx.execute()  // blocking
    val res = tx.getResponse().asInstanceOf[ReadMultipleRegistersResponse]
    val results = res.getRegisters.map(_.toUnsignedShort)
    results.foreach(println)
    println

    sender ! Result(LocalDateTime.now(), "succcess")
  }

}
