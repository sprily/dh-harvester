package uk.co.sprily.dh
package harvester

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.ReceiveTimeout

import network.Device

class PollingActor[D <: Device](
    val req: PersistentRequest[D],
    val directory: DeviceActorDirectoryService[D]) extends Actor
                                                 with ActorLogging {

  import PollingActor._
  import PollingActor.Protocol._
  import directory.Protocol._

  // Type alias for convenience, it's still path dependent
  type Target = req.schedule.Target

  // akka stuff
  private implicit val dispatcher =  context.system.dispatcher
  private val scheduler = context.system.scheduler

  // our actor's state
  private var currentTarget: Option[Target] = None

  def receive = {
    case StartActor       => startActorRcvd()
    case PollNow          => pollNowRcvd()
    case ReceiveTimeout   => receiveTimeoutRcvd()
    case m@PollResult(ts) => pollResultRcvd(m)
  }

  /* Responses to messages */

  def startActorRcvd() = {
    log.debug(s"Starting PollingActor: ${self}")
    currentTarget = Some(req.schedule.start())
    schedulePollFor(currentTarget.get)
  }

  def pollNowRcvd() = {
    log.debug(s"Polling device ${req.device}")
    currentTarget.foreach { target =>
      context.setReceiveTimeout(target.timeoutDelay())
      gateway ! pollMsg
    }
  }

  def pollResultRcvd(m: PollResult) = {
    log.debug(s"Received PollResult from gateway: ${m.timestamp}")
    // TODO: send this result somewhere
    currentTarget.foreach { target =>
      currentTarget = Some(req.schedule.completed(target))
      schedulePollFor(target)
    }
  }

  def receiveTimeoutRcvd() = {
    log.warning(s"Timed-out out waiting for response from ${req.device}")
    currentTarget.foreach { target =>
      currentTarget = Some(req.schedule.timedOut(target))
    }
    throw new PollingTimedOutException()
  }

  /* Helper methods */

  private def schedulePollFor(target: Target): Unit = {
    scheduler.scheduleOnce(target.initialDelay(), self, PollNow)
  }

  private def gateway = directory.lookup(req.device)

  private val pollMsg = Poll(req.device, req.selection)
}

object PollingActor {

  def props[D <: Device](req: PersistentRequest[D])
                        (implicit directory: DeviceActorDirectoryService[D]): Props = {

    Props(new PollingActor[D](req, directory))

  }

  class PollingTimedOutException extends Exception

  object Protocol {
    case object StartActor
    case object PollNow
  }
  
}
