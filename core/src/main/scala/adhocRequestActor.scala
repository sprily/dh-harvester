package uk.co.sprily.dh
package harvester
package capture

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.ReceiveTimeout

import network.Device

class AdhocRequestActor[D <: Device](
    val req: AdhocRequest[D],
    val directory: DeviceActorDirectory[D],
    val bus: DeviceBus) extends Actor
                           with ActorLogging {

  import AdhocRequestActor._
  import AdhocRequestActor.Protocol._
  import directory.Protocol._

  // akka stuff
  private implicit val dispatcher =  context.system.dispatcher
  private val scheduler = context.system.scheduler

  schedulePoll()

  def receive = {
    case PollNow          => pollNowRcvd()
    case ReceiveTimeout   => receiveTimeoutRcvd()
    case r@Result(_,_)    => resultRcvd(r)
  }

  /* Responses to messages */

  def pollNowRcvd() = {
    log.debug(s"Polling device ${req.device}")
    context.setReceiveTimeout(req.target.timeoutDelay())
    gateway ! pollMsg
  }

  def resultRcvd(r: Result) = {
    log.debug(s"Received PollResult from gateway: ${r.timestamp}")
    context.setReceiveTimeout(Duration.Undefined)
    val reading = Reading(r.timestamp, req.device, r.measurement)
    bus.publish(reading)
    context.stop(self)
  }

  def receiveTimeoutRcvd() = {
    log.warning(s"Timed-out out waiting for response from ${req.device}")
    context.setReceiveTimeout(Duration.Undefined)
    throw new RequestTimedOutException()
  }

  /* Helper methods */

  private def schedulePoll(): Unit = {
    scheduler.scheduleOnce(req.target.initialDelay(), self, PollNow)
  }

  private def gateway = directory.lookup(req.device)

  private val pollMsg = Poll(req.device, req.selection)
}

object AdhocRequestActor {

  def props[D <: Device](req: AdhocRequest[D])
                        (implicit directory: DeviceActorDirectory[D],
                                  bus: DeviceBus): Props = {
    Props(new AdhocRequestActor[D](req, directory, bus))
  }

  class RequestTimedOutException extends Exception

  object Protocol {
    case object PollNow
  }
  
}
