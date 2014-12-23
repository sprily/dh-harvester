package uk.co.sprily.dh
package harvester
package modbus

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._
import akka.routing.RoundRobinPool
import akka.util.ByteString

import capture.GatewayActorDirectory
import network.TCPGateway

object ModbusGatewayActor {

  import GatewayActorDirectory.Protocol.Register

  def registerWithDirectory(system: ActorSystem): Unit = {
    val directory = system.actorSelection(s"/user/${GatewayActorDirectory.name}")
    directory ! Register {
      case (gw: TCPGateway) => props(gw, numConnections=1)    // TODO: change to ModbusGateway
    }
  }

  /** A pool of ModbusGatewayActors **/
  def props(gw: TCPGateway, numConnections: Int = 1): Props = {
    singleProps(gw).
      withRouter(RoundRobinPool(
        numConnections,
        supervisorStrategy=supervisorStrategy))
  }

  private def singleProps(gw: TCPGateway): Props = {
    Props(new ModbusGatewayActor(gw))
  }

  private lazy val supervisorStrategy = OneForOneStrategy() {
    case e: Exception => Restart
  }

}

/** Manages a *single* connection to a modbus TCP gateway */
class ModbusGatewayActor(gateway: TCPGateway) extends Actor 
                                                 with ActorLogging {

  import com.ghgande.j2mod.modbus._
  import com.ghgande.j2mod.modbus.msg.{ModbusRequest => _, ModbusResponse => _, _}
  import com.ghgande.j2mod.modbus.io._
  import com.ghgande.j2mod.modbus.net._
  import com.ghgande.j2mod.modbus.util._

  private[this] var conn: TCPMasterConnection = _

  /** Akka Actor override **/
  override def postStop() = {
    if (conn != null) {
      conn.close()
    }
    conn = null
  }

  def receive = {
    case (r: ModbusRequest) => blockingRequest(r)
  }

  private[this] def blockingRequest(r: ModbusRequest): Unit = {

    connectIfNecessary()

    val req = new ReadMultipleRegistersRequest(
      r.selection.startRegister,
      r.selection.numRegisters)
    req.setUnitID(r.device.address.deviceNumber)

    val tx = new ModbusTCPTransaction(conn)
    tx.setRequest(req)

    log.debug(s"Executing modbus transaction: ${r.selection}")
    tx.execute()  // blocking -- TODO: may raise NullPointerException if connection lost
    log.debug(s"Transaction completed")
    val res = tx.getResponse().asInstanceOf[ReadMultipleRegistersResponse]
    log.debug(s"RES: $res") // TODO: res == null => failed transaction (connection accepted, but not completed?)
    val m = ModbusMeasurement(
      r.selection,
      res.getRegisters.map(reg => ByteString(reg.toBytes)).reduce(_ ++ _))

    sender ! ModbusResponse(r.device, m)
  }

  private[this] def connectIfNecessary(): Unit = {
    if (conn == null) {
      log.info(s"Establishing initial connection to ${gateway}")
      conn = new TCPMasterConnection(gateway.address.inet)
      conn.setPort(gateway.port)
      conn.connect()
    }
  }

}
