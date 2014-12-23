package uk.co.sprily.dh
package harvester
package capture

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.ReceiveTimeout

import org.joda.time.LocalDateTime

import network.Device

import scheduling.Schedule
import scheduling.TargetLike

trait Request2 {
  type D <: Device
  type Selection = D#AddressSelection

  val device: D
  val selection: Selection
}

trait Response2 {
  type D <: Device
  type Measurement = D#Measurement

  val timestamp: LocalDateTime
  val device: D
  val measurement: Measurement
}

class RequestActor2(
    val request: Request2,
    val schedule: Schedule,
    val bus: DeviceBus) extends Actor with ActorLogging {

  import RequestActor2._
  import RequestActor2.Protocol._
  import DeviceActorDirectory2.{Protocol => DeviceProtocol}

  private val device = request.device
  private val deviceActor = context.actorSelection(s"/user/${DeviceActorDirectory2.name}")

  /** Akka stuff **/
  private implicit val dispatcher = context.system.dispatcher
  private val scheduler = context.system.scheduler

  private var target = schedule.start()
  scheduleNextPoll()

  def receive = {
    case PollNow        => pollNowRcvd()
    case ReceiveTimeout => timeoutRcvd()
    case (r: Response2) => responseRcvd(r)
  }

  protected def pollNowRcvd() = {
    log.debug(s"Polling device $device")
    context.setReceiveTimeout(target.timeoutDelay())
    deviceActor ! DeviceProtocol.Forward(device, request)
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

    //throw new PollingTimedOutException()
  }

  protected def responseRcvd(r: Response2) = {
    log.debug(s"Received response from device at ${r.timestamp}")
    context.setReceiveTimeout(Duration.Undefined)
    // bus.publish(r)
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

object RequestActor2 {

  def props(request: Request2, schedule: Schedule, bus: DeviceBus): Props = Props(
    new RequestActor2(request, schedule, bus)
  )

  protected[capture] case object Protocol {
    case object PollNow
  }

  class PollingTimedOutException extends Exception
}
