package uk.co.sprily.dh
package harvester

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.ReceiveTimeout

import org.joda.{time => joda}

import network.Device

class PollingActor[D <: Device](
    val req: PersistentRequest[D],
    val directory: DeviceDirectoryService[D]) extends Actor
                                                 with ActorLogging {

  import PollingActor._
  import directory.Protocol._

  private implicit val dispatcher =  context.system.dispatcher
  private val scheduler = context.system.scheduler

  private var schedule: req.target.Schedule = _

  def receive = {

    case StartActor => {
      log.debug(s"Starting PollingActor: ${self}")
      schedule = req.target.start
      scheduleAgain()
    }

    case PollNow(timeout: FiniteDuration) => {
      log.debug(s"Polling device ${req.device}")
      context.setReceiveTimeout(schedule.timeout)
      gateway ! pollMsg
    }

    case ReceiveTimeout => {
      log.warning(s"Timed-out out waiting for response from ${req.device}")
      schedule = req.target.timedOut(schedule)
      throw new PollingTimedOutException()
    }

    case PollResult(ts) => {
      log.debug(s"Received PollResult from gateway: ${ts}")
      // TODO: send this result somewhere
      schedule = req.target.completed(schedule)
      scheduleAgain()
    }
  }

  private def scheduleAgain(): Unit = {
    scheduleIn(schedule.delay, schedule.timeout)
  }

  private def scheduleIn(duration: FiniteDuration, timeout: FiniteDuration) = {
    scheduler.scheduleOnce(duration, self, PollNow(timeout))
  }

  private def gateway = directory.lookup(req.device)

  private val pollMsg = Poll(req.device, req.selection)

  sealed trait Protocol
  case object StartActor extends Protocol
  case class PollNow(timeout: FiniteDuration) extends Protocol
  
}

object PollingActor {

  def props[D <: Device](req: PersistentRequest[D])
                        (implicit directory: DeviceDirectoryService[D]): Props = {

    Props(new PollingActor[D](req, directory))

  }

  class PollingTimedOutException extends Exception
}
