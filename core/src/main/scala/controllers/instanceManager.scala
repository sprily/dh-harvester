package uk.co.sprily.dh
package harvester
package controllers

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props

import capture.RequestLike
import capture.RequestActorManager
import network.DeviceId
import network.DeviceLike
import scheduling.Schedule

trait DeviceManagerProvider {
  def deviceManagerProps: Props
}

class InstanceManager extends Actor
                         with ActorLogging {
  this: DeviceManagerProvider =>

  import InstanceManager.Protocol._
  import RequestActorManager.Protocol._

  def receive = {
    case (c: InstanceConfig) => setConfig(c)
    case AdhocRequest(r)     => sendAdhocRequest(r)
  }

  private[this] def setConfig(c: InstanceConfig) = {
    deviceManager ! c.devices
    scheduledRequestsManager ! c.requests
  }

  private[this] def sendAdhocRequest(r: RequestLike) = {
    adhocMgr forward r
  }

  private[this] def scheduledRequestsManager = context.actorSelection("TODO")
  private[this] def deviceManager            = context.actorSelection("TODO")
  private[this] def adhocMgr                 = context.actorSelection("TODO")

}

object InstanceManager {

  def name = "instance-manager"
  def props = Props(new InstanceManager() with DeviceManagerProvider {
    override def deviceManagerProps = ???
  })

  object Protocol {
    case class AdhocRequest(request: RequestLike)
    case class InstanceConfig(requests: Seq[RequestLike]) {
      def devices = requests.map(_.device).distinct
    }
  }

}
