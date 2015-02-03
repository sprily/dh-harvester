package uk.co.sprily.dh
package harvester
package capture

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ReceiveTimeout

import controllers.DeviceManager
import network.DeviceLike
import scheduling.Schedule
import scheduling.TargetLike

class RequestActor(
    val request: RequestLike,
    val schedule: Schedule,
    val bus: ResponseBus,
    val deviceManager: ActorRef) extends Actor with ActorLogging {

  import RequestActor._
  import RequestActor.Protocol._

  private val device = request.device

  /** Akka stuff **/
  private implicit val dispatcher = context.system.dispatcher
  private val scheduler = context.system.scheduler

  private var target = schedule.start()
  scheduleNextPoll()

  def receive = {
    case PollNow        => pollNowRcvd()
    case ReceiveTimeout => timeoutRcvd()
    case (r: ResponseLike)  => responseRcvd(r)
  }

  protected def pollNowRcvd() = {
    log.info(s"Polling device $device")
    context.setReceiveTimeout(target.timeoutDelay())
    deviceManager ! request
  }

  protected def timeoutRcvd() = {
    log.warning(s"Timed-out waiting for response from $device")
    context.setReceiveTimeout(Duration.Undefined)
    schedule.timedOut(target) match {
      case None =>
        log.info(s"Stopping RequestActor $request as Schedule completed")
        context.stop(self)
      case Some(t) =>
        target = t
        scheduleNextPoll()
    }

    throw new PollingTimedOutException()
  }

  protected def responseRcvd(r: ResponseLike) = {
    log.info(s"Received response from device at ${r.timestamp}")
    context.setReceiveTimeout(Duration.Undefined)
    bus.publish(r)
    schedule.completed(target) match {
      case None =>
        log.info(s"Stopping RequestActor $request as Schedule completed")
        context.stop(self)
      case Some(t) =>
        target = t
        scheduleNextPoll()
    }
  }

  protected def scheduleNextPoll(): Unit = {
    scheduler.scheduleOnce(target.initialDelay(), self, PollNow)
  }

}

object RequestActor {

  def props(request: RequestLike,
            schedule: Schedule,
            bus: ResponseBus,
            deviceManager: ActorRef): Props = Props(
    new RequestActor(request, schedule, bus, deviceManager)
  )

  protected[capture] case object Protocol {
    case object PollNow
  }

  class PollingTimedOutException extends Exception
}
