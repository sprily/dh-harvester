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
    val gwService: GatewayService[D]) extends Actor
                                         with ActorLogging {

  import PollingActor._
  import gwService.Protocol._

  private implicit val dispatcher =  context.system.dispatcher
  private val scheduler = context.system.scheduler

  private var pollSentAt = joda.LocalDateTime.now()

  override def preStart() = {
    setReceiveTimeout()
    self ! PollNow
  }

  def receive = {

    case PollNow => {
      log.debug(s"Polling device ${req.device}")
      pollSentAt = joda.LocalDateTime.now()
      gateway ! pollMsg
    }

    case ReceiveTimeout => {
      log.warning(s"Timed-out out waiting for response from ${req.device}")
      throw new PollingTimedOutException()
    }

    case PollResult(ts) => {
      log.debug(s"Received PollResult from gateway: ${ts}")
      // TODO: send this result somewhere
      req.target match {
        case Every(_, _)  => scheduleAgain()
        case Within(_, _) => scheduleAgain()
        case _            => { }
      }
    }
  }

  private def scheduleAgain(): Unit = {
    val now = joda.LocalDateTime.now()
    val diff = new joda.Duration(pollSentAt.toDateTime, now.toDateTime).getMillis.millis

    req.target match {
      case Every(interval, _) => {
        if (diff >= interval) {
          log.warning(s"Missed target for polling ${req.device} [target: ${req.target}]")
          self ! PollNow
        } else {
          scheduleIn(interval - diff)
        }
      }

      case t@Within((lower, upper), _) => {
        if (diff >= upper) {
          log.warning(s"Missed target for polling ${req.device} [target: ${req.target}]")
          self ! PollNow
        } else if (diff >= lower) {
          self ! PollNow
        } else {
          scheduleIn(t.midpoint - diff)
        }
      }

      case t => { }
    }
  }

  private def scheduleIn(duration: FiniteDuration) = {
    scheduler.scheduleOnce(duration, self, PollNow)
  }

  private def gateway = gwService.lookup(req.device)

  private def setReceiveTimeout(): Unit = {
    context.setReceiveTimeout(req.target.timeout)
  }

  private val pollMsg = Poll(req.device, req.selection)

  sealed trait Protocol
  case object PollNow extends Protocol
  
}

object PollingActor {
  class PollingTimedOutException extends Exception
}
