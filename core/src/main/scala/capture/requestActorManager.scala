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
import network.Device

/** Manages the set of active RequestActors **/
class RequestActorManager(bus: DeviceBus) extends Actor
                                             with ActorLogging {

  import RequestActorManager.Child
  import RequestActorManager.Protocol._
  import RequestActor.PollingTimedOutException

  // Actor state
  var requests = Map[Long, Child]()

  def receive = {
    case (r: Request)             => ensureRequest(r)
    case (rs: PersistentRequests) => ensurePersistentRequests(rs)
    case Terminated(a)            => handleChildTerminated(a)
  }

  def ensureRequest(r: Request) = {
    log.info(s"Ensuring request $r")
    requests.get(r.id) match {
      case None                        => spawnChild(r)
      case Some(child) if child.r != r => replaceChild(r)
      case _                           => ()
    }
  }

  def ensurePersistentRequests(rs: PersistentRequests) = {
    log.info(s"Ensuring PersistentRequests are set to run")
    val activeIds = rs.requests.map(_.id).toSet
    requests.foreach { case (id, child) =>
      if (! activeIds.contains(id)) stopRequest(child.r)
    }
    rs.requests foreach ensureRequest
  }

  final private def spawnChild(r: Request) = {
    val ref = context.actorOf(RequestActor.props(r, ???, bus), r.id.toString)
    requests = requests + (r.id -> Child(r, ref))
    context.watch(ref)
  }

  final private def replaceChild(r: Request) = {
    requests.get(r.id).foreach { _.ref ! PoisonPill.getInstance }
    spawnChild(r)
  }

  final private def stopRequest(r: Request) = {
    log.info(s"Stopping $r")
    requests.get(r.id).foreach { child =>
      child.ref ! PoisonPill.getInstance
      requests = requests - r.id
    }
  }

  final private def handleChildTerminated(a: ActorRef) = {
    log.info(s"Child RequestActot terminated $a")
    requests = requests.filter { case (_, Child(_, ref)) => a != ref }
  }

}

object RequestActorManager {

  protected case class Child(r: Request, ref: ActorRef)

  object Protocol {
    case class PersistentRequests(requests: Seq[Request])
  }
}
