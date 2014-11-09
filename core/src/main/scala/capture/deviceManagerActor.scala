package uk.co.sprily.dh
package harvester
package capture

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.OneForOneStrategy
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.actor.SupervisorStrategy._
import akka.actor.Terminated

import modbus.ModbusDevice

class DeviceManagerActor(
    provider: DirectoryProvider,
    implicit val bus: DeviceBus)
    extends Actor with ActorLogging {

  import DeviceManagerActor.Protocol._
  import RequestActor.PollingTimedOutException

  override val supervisorStrategy = OneForOneStrategy() {
    case _: PollingTimedOutException => Resume
  }

  case class Child(dr: DeviceRequest, ref: ActorRef)
  var requests = Map[Long, Child]()

  def receive = {
    case (r: ModbusRequest)       => ensureRequest(r)
    case (rs: PersistentRequests) => ensurePersistentRequests(rs)
    case Terminated(a)            => handleChildTerminated(a)
  }

  def ensurePersistentRequests(rs: PersistentRequests) = {
    log.info(s"Ensuring PersistentRequests are set to run")
    val activeIds = rs.requests.map(_.r.id).toSet
    requests.foreach { case (id, child) =>
      if (! activeIds.contains(id)) stopRequest(child.dr)
    }
    rs.requests foreach ensureRequest
  }

  def ensureRequest(dr: DeviceRequest) = {
    log.info(s"Ensuring DeviceRequest $dr")
    dr match {
      case ModbusRequest(r) =>
        implicit val modbus = provider.lookup[ModbusDevice].get
        if (! requests.contains(r.id)) {
          val ref = context.actorOf(RequestActor.props(r))
          requests = requests + (r.id -> Child(dr, ref))
          context.watch(ref)
        } else if (requests(r.id).dr != dr) {
          requests(r.id).ref ! PoisonPill.getInstance
          val ref = context.actorOf(RequestActor.props(r))
          requests = requests + (r.id -> Child(dr, ref))
          context.watch(ref)
        }
    }
  }

  def stopRequest(dr: DeviceRequest) = {
    log.info(s"Stopping request $dr.r.id")
    dr match {
      case ModbusRequest(r) =>
        if (requests.contains(r.id)) {
          val child = requests(r.id)
          child.ref ! PoisonPill.getInstance
          requests = requests - r.id
        }
    }
  }

  def handleChildTerminated(a: ActorRef) = {
    log.info(s"Child RequestActor terminated $a")
    requests = requests.filter { case (_, Child(_, ref)) => a != ref  }
  }

}

object DeviceManagerActor {
  object Protocol {

    sealed trait DeviceRequest { def r: Request[_] }
    case class ModbusRequest(r: Request[ModbusDevice]) extends DeviceRequest

    case class PersistentRequests(requests: Seq[DeviceRequest])
  }
}
