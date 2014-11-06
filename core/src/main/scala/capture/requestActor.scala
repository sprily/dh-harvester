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

trait RequestActor[D <: Device] extends Actor with ActorLogging {

  val bus: DeviceBus
  val directory: DeviceActorDirectory[D]
  val request: RequestLike[D]

  def target: TargetLike

  import RequestActor.Protocol._
  import directory.Protocol._

  // akka stuff
  private implicit val dispatcher =  context.system.dispatcher
  private val scheduler = context.system.scheduler

  def receive = {
    case PollNow        => pollNowRcvd()
    case ReceiveTimeout => receiveTimeoutRcvd()
    case r@Result(_,_)  => resultRcvd(r)
  }

  protected def pollNowRcvd() = {
    log.debug(s"Polling device ${request.device}")
    context.setReceiveTimeout(target.timeoutDelay())
    gateway ! pollMsg
  }

  protected def receiveTimeoutRcvd() = ???
  protected def resultRcvd(r: Result) = ???

  private def gateway = directory.lookup(request.device)
  private val pollMsg = Poll(request.device, request.selection)
}

object RequestActor {
  protected[capture] case object Protocol {
    case object PollNow
  }
}
