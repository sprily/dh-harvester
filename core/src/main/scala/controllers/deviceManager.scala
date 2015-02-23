package uk.co.sprily.dh
package harvester
package controllers

import scala.Function.const

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props

import actors.ActorDirectory
import capture.RequestLike
import network.DeviceId
import network.DeviceLike

/** Manages the devices that this instance communicates with.
  *
  */
class DeviceManager extends Actor with ActorLogging {

  import DeviceManager._
  import DeviceManager.Protocol._

  private[this] var devices = Map.empty[DeviceId, DeviceLike]
  private[this] var children = Map.empty[DeviceId, ActorRef]
  private[this] var deviceTypes: PropsFactory = Map.empty

  def receive = {
    case SetDevices(ds)       => setDevices(ds)
    case Register(deviceType) => registerDeviceType(deviceType)
    case (r: RequestLike)     => forwardRequest(r)
  }

  private[this] def setDevices(ds: Seq[DeviceLike]) = {
    log.info(s"Setting devices to: $ds")
    val existing = devices.values.toSet
    val toRemove = devices.values.filter(!ds.contains(_))
    val toAdd = ds.filter(!existing.contains(_))

    toRemove foreach stopChild
    toAdd foreach spawnChild
    devices = Map(ds.map { d => d.id -> d }:_*)
  }

  private[this] def stopChild(d: DeviceLike) = {
    children.get(d.id).foreach { _ ! PoisonPill.getInstance }
    children = children.filterKeys(_ != d.id)
  }

  private[this] def spawnChild(d: DeviceLike) = {
    deviceTypes.lift(d).foreach { props =>
      val ref = context.actorOf(props)
      children = children + (d.id -> ref)
    }
  }

  private[this] def forwardRequest(r: RequestLike) = {
    children.get(r.device.id) match {
      case Some(child) => child forward r
      case None        => 
        log.warning(s"Unable to forward request: $r")
        sender ! UnknownDevice(r.device)
    }
  }

  private[this] def registerDeviceType(deviceType: PropsFactory) = {
    deviceTypes = deviceType orElse deviceTypes
  }

}

object DeviceManager {

  type PropsFactory = PartialFunction[DeviceLike, Props]

  object Protocol {
    case class SetDevices(devices: Seq[DeviceLike])
    case class Register(props: PropsFactory)

    case class UnknownDevice(device: DeviceLike)
  }

  def name = "device-manager"
  def props = Props(new DeviceManager())

}
