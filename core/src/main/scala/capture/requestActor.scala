package uk.co.sprily.dh
package harvester
package capture

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.ReceiveTimeout

import network.Device
import scheduling.TargetLike

class RequestActor[D <: Device](
    val request: Request[D],
    val directory: DeviceActorDirectory[D],
    val bus: DeviceBus) extends Actor with ActorLogging {

  // actor state
  private var target = request.schedule.start()

  import RequestActor._
  import RequestActor.Protocol._
  import directory.Protocol._

  // akka stuff
  private implicit val dispatcher = context.system.dispatcher
  private val scheduler = context.system.scheduler

  scheduleNextPoll()

  def receive = {
    case PollNow        => pollNowRcvd()
    case ReceiveTimeout => receiveTimeoutRcvd()
    case (r: Result)    => resultRcvd(r)
  }

  protected def pollNowRcvd() = {
    log.debug(s"Polling device ${request.device}")
    context.setReceiveTimeout(target.timeoutDelay())
    gateway ! pollMsg
  }

  protected def receiveTimeoutRcvd() = {
    log.warning(s"Timed-out out waiting for response from ${request.device}")
    context.setReceiveTimeout(Duration.Undefined)
    request.schedule.timedOut(target) match {
      case None =>
        log.info(s"Stopping RequestActor (${request}) as Schedule completed")
        context.stop(self)
      case Some(t) =>
        target = t
        scheduleNextPoll()
    }

    // TODO: Should we throw the time-out exception when the schedule completes?
    throw new PollingTimedOutException()
  }

  protected def resultRcvd(r: Result) = {
    log.debug(s"Received PollResult from gateway: ${r.timestamp}")
    context.setReceiveTimeout(Duration.Undefined)
    val reading = Reading(r.timestamp, request.device, r.measurement)
    bus.publish(reading)
    request.schedule.timedOut(target) match {
      case None =>
        log.info(s"Stopping RequestActor (${request}) as Schedule completed")
        context.stop(self)
      case Some(t) =>
        target = t
        scheduleNextPoll()
    }
  }

  protected def scheduleNextPoll(): Unit = {
    scheduler.scheduleOnce(target.initialDelay(), self, PollNow)
  }

  private def gateway = directory.lookup(request.device)
  private val pollMsg = Poll(request.device, request.selection)
}

object RequestActor {

  def props[D <: Device](req: Request[D])
                        (implicit directory: DeviceActorDirectory[D],
                                  bus: DeviceBus): Props = {

    Props(new RequestActor[D](req, directory, bus))

  }

  protected[capture] case object Protocol {
    case object PollNow
  }

  class PollingTimedOutException extends Exception
}
