package uk.co.sprily.dh
package harvester
package capture

import scala.concurrent.duration._
import scala.reflect.runtime.universe._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.ReceiveTimeout

import network.Device

import modbus.ModbusDevice
import modbus.ModbusActorDirectory

class DeviceManagerActor(implicit val bus: DeviceBus)
    extends Actor with ActorLogging {

  import DeviceManagerActor.Protocol._

  // Directories are initialised in preStart()
  implicit private var modbusDirectory: DeviceActorDirectory[ModbusDevice] = _

  case class Child(dr: DeviceRequest, ref: ActorRef)
  var requests = Map[Long, Child]()

  // Override postRestart to ensure preStart() is only called once.
  override def postRestart(reason: Throwable): Unit = ()

  override def preStart(): Unit = {
    modbusDirectory = new ModbusActorDirectory(context.system)
  }

  def receive = {
    case (r: ModbusRequest)       => ???
    case (rs: PersistentRequests) => ???
  }

  def ensurePersistentRequests(rs: PersistentRequests) = {
    val activeIds = rs.requests.map(_.r.id).toSet
    requests.foreach { case (id, child) =>
      if (! activeIds.contains(id)) stopRequest(child.dr)
    }
    rs.requests foreach ensureRequest
  }

  def ensureRequest(dr: DeviceRequest) = {
    dr match {
      case ModbusRequest(r) =>
        if (! requests.contains(r.id)) {
          val ref = context.actorOf(RequestActor.props(r))
          requests = requests + (r.id -> Child(dr, ref))
        } else if (requests(r.id).dr != dr) {
          requests(r.id).ref ! PoisonPill.getInstance
          val ref = context.actorOf(RequestActor.props(r))
          requests = requests + (r.id -> Child(dr, ref))
        }
    }
  }

  def stopRequest(dr: DeviceRequest) = {
    dr match {
      case ModbusRequest(r) =>
        if (requests.contains(r.id)) {
          val child = requests(r.id)
          child.ref ! PoisonPill.getInstance
          requests = requests - r.id
        }
    }
  }

}

object DeviceManagerActor {
  object Protocol {

    sealed trait DeviceRequest { def r: Request[_] }
    case class ModbusRequest(r: Request[ModbusDevice]) extends DeviceRequest

    case class PersistentRequests(requests: Seq[DeviceRequest])
  }
}
