package uk.co.sprily.dh
package harvester
package controllers

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.OneForOneStrategy
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.actor.Terminated

import capture.RequestLike
import modbus.ModbusDevice
import scheduling.Schedule

/** Manages the set of active RequestActors **/
class RequestManager(
    bus: ResponseBus,
    deviceManager: ActorRef) extends Actor with ActorLogging {

  import RequestManager.Child
  import RequestManager.Protocol._

  // Actor state
  var requests = Map[Long, Child]()

  def receive = {
    case (r: RequestLike)         => ensureRequest(ScheduledRequest(r, once))
    case (rs: PersistentRequests) => ensurePersistentRequests(rs)
    case Terminated(a)            => handleChildTerminated(a)
  }

  def ensureRequest(r: ScheduledRequest) = {
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

  final private def spawnChild(r: ScheduledRequest) = {
    val ref = context.actorOf(RequestActor.props(r.request, r.schedule, bus, deviceManager))
    requests = requests + (r.id -> Child(r.request, ref))
    context.watch(ref)
  }

  final private def replaceChild(r: ScheduledRequest) = {
    requests.get(r.id).foreach { _.ref ! PoisonPill.getInstance }
    spawnChild(r)
  }

  final private def stopRequest(r: RequestLike) = {
    log.info(s"Stopping $r")
    requests.get(r.id).foreach { child =>
      child.ref ! PoisonPill.getInstance
      requests = requests - r.id
    }
  }

  final private def handleChildTerminated(a: ActorRef) = {
    log.info(s"Child RequestActor terminated $a")
    requests = requests.filter { case (_, Child(_, ref)) => a != ref }
  }

  final private def once = Schedule.single(60.seconds)

}

object RequestManager {

  def props(bus: ResponseBus, deviceManager: ActorRef) = Props(
    new RequestManager(bus, deviceManager)
  )

  protected case class Child(r: RequestLike, ref: ActorRef)

  object Protocol {

    case class ScheduledRequest(request: RequestLike, schedule: Schedule) {
      def id = request.id
    }

    case class PersistentRequests(requests: Seq[ScheduledRequest])
  }
}
