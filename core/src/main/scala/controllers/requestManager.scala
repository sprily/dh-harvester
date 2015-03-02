package uk.co.sprily.dh
package harvester
package controllers

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.OneForOneStrategy
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.actor.Terminated

import capture.ScheduledRequestLike
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
  private var persistentRequests = Map[ScheduledRequestLike, Child]()
  private var adhocRequests      = Map[RequestLike, Child]()

  def receive = {
    //case AdhocRequest(r, timeout) => adhocRequest(r, timeout, sender)
    case PersistentRequests(rs)   => setPersistent(rs)
    case Terminated(a)            => handleTermination(a)
    case other => log.warning(s"Unhandled: $other")
  }

  private[this] def setPersistent(reqs: Seq[ScheduledRequestLike]) = {

    log.info(s"Setting persistent requests: $reqs")

    persistentRequests.foreach { case (schReq, child) =>
      if (! reqs.contains(schReq)) { stopChild(child) }
    }

    reqs.foreach { schReq =>
      persistentRequests.get(schReq) match {
        case None        => spawn(schReq)
        case Some(child) => ()
      }
    }
  }

  private[this] def spawn(sr: ScheduledRequestLike) = {
    log.info(s"Spawning child: $sr")
    val (request, schedule) = sr
    val ref = context.actorOf(RequestActor.props(request, schedule, bus, deviceManager))
    persistentRequests = persistentRequests + (sr -> Child(sr, ref))
    context.watch(ref)
  }

  private[this] def stopChild(child: Child) = {
    log.info(s"Stopping child $child")
    child.ref ! PoisonPill.getInstance
  }

  private[this] def handleTermination(a: ActorRef) = {
    log.info(s"Child RequestActor terminated $a")
    persistentRequests = persistentRequests.filter { case (_, Child(_, ref)) => a != ref }
  }

}

object RequestManager {

  def props(bus: ResponseBus, deviceManager: ActorRef) = Props(
    new RequestManager(bus, deviceManager)
  )

  protected case class Child(sr: ScheduledRequestLike, ref: ActorRef)

  object Protocol {
    case class PersistentRequests(requests: Seq[ScheduledRequestLike])
  }
}
