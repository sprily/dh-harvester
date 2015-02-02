package uk.co.sprily.dh
package harvester
package controllers

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props

import scalaz._

import capture.RequestLike
import capture.RequestActorManager
import network.DeviceId
import network.DeviceLike
import scheduling.Schedule

/** Manages the configuration of this running instance.
  *
  * Responsibilities:
  *
  *  - keeping track of configured Devices
  *  - ensuring that requests are only sent to configured devices
  */
class InstanceManager extends Actor with ActorLogging {

  import InstanceManager.Protocol._
  import RequestActorManager.Protocol._

  private[this] var devices = Map.empty[DeviceId, DeviceLike]

  def receive = {
    case (c: InstanceConfig) => setConfig(c)
  }

  private[this] def setConfig(c: InstanceConfig) = {
    devices = c.devices.map { d => (d.id -> d) }.toMap
    val requests = c.requests.map { req => ScheduledRequest(req._1, req._2) }
    requestManager ! PersistentRequests(requests)
  }

  private[this] def requestManager = context.actorSelection("TODO")

}

object InstanceManager {

  object Protocol {

    case class InstanceConfig(managedDevices: Seq[ManagedDevice]) {
      def devices = managedDevices.map(_.device)
      def requests = managedDevices.flatMap(_.requests)
    }

    trait ManagedDevice {
      type Request <: RequestLike
      type ScheduledRequest = (Request, Schedule)
      type Device <: Request#Device
      def device: Device
      def requests: Seq[ScheduledRequest]
    }

    trait Scheduled {
      val schedule: Schedule
    }
  }

}
