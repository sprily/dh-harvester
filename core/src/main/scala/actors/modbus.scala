package uk.co.sprily.dh
package harvester
package modbus

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._
import akka.routing.RoundRobinPool
import akka.util.ByteString

import org.joda.time.LocalDateTime
import org.joda.time.DateTimeZone

import capture.DeviceActorDirectory2
import capture.Request2
import capture.Response2
import capture.GatewayActorDirectory

import network.TCPGateway

case class ModbusRequest(
    device: ModbusDevice,
    selection: ModbusRegisterRange) extends Request2 {
  type D = ModbusDevice
}

case class ModbusResponse(
    device: ModbusDevice,
    measurement: ModbusMeasurement,
    timestamp: LocalDateTime) extends Response2 {
  type D = ModbusDevice
}

object ModbusResponse {
  def apply(d: ModbusDevice, m: ModbusMeasurement): ModbusResponse = {
    ModbusResponse(d, m, LocalDateTime.now(DateTimeZone.UTC))
  }
}


/** An Actor representing a single ModbusDevice.
  *
  * It processes `ModbusRequest` tasks by forwarding the request to the
  * appropriate gateway (through a `GatewayActorDirectory` service actor).
  */
class ModbusDeviceActor(
    device: ModbusDevice) extends Actor
                             with ActorLogging {

  import GatewayActorDirectory.Protocol.Forward

  private lazy val gateway = context.actorSelection(s"/user/${GatewayActorDirectory.name}")

  def receive = {
    case req@ModbusRequest(device, selection) =>
      gateway ! Forward(device.address.gateway, req)
  }

}

object ModbusDeviceActor {

  import DeviceActorDirectory2.Protocol.Register

  def registerWithDirectory(system: ActorSystem): Unit = {
    val directory = system.actorSelection(s"/user/${DeviceActorDirectory2.name}")
    directory ! Register {
      case (d: ModbusDevice) => props(d)
    }
  }

  def props(d: ModbusDevice) = Props(new ModbusDeviceActor(d))

}

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
    tx.execute()  // blocking
    log.debug(s"Transaction completed")
    val res = tx.getResponse().asInstanceOf[ReadMultipleRegistersResponse]
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
