package uk.co.sprily.dh
package harvester
package controllers

import scala.Function.const

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props

import scalaz._
import scalaz.std.list._
import scalaz.syntax.traverse._

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
    case Lookup(id)           => lookupDevice(id)
    case SetDevices(ds)       => setDevices(ds)
    case Register(deviceType) => registerDeviceType(deviceType)
    case (r: RequestLike)     => forwardRequest(r)
  }

  private[this] def lookupDevice(id: DeviceId) = {
    sender ! devices.get(id)
  }

  private[this] def forwardRequest(r: RequestLike) = {
    children.get(r.device.id) match {
      case Some(child) => child forward r
      case None        =>
        log.warning(s"Unable to forward request: $r")
    }
  }

  private[this] def setDevices(ds: Seq[DeviceLike]) = {
    val existing = devices.values.toSet
    val toRemove = devices.values.filter(!ds.contains(_))
    val toAdd = ds.filter(!existing.contains(_))

    def reset() = {
      toRemove foreach stopChild
      toAdd foreach spawnChild
      devices = Map(ds.map { d => d.id -> d }:_*)
    }

    val valid = validateDevices(toAdd)
    if (valid.isSuccess) reset()
    sender ! valid.map(const(ds))
  }

  private[this] def validateDevices(ds: Seq[DeviceLike]) = {

    def validate(d: DeviceLike) = {
      deviceTypes.isDefinedAt(d) match {
        case true  => success(d)
        case false => failure(s"Device type not supported: $d")
      }
    }

    val duplicates = (ds.map(_.id).distinct.size match {
      case size if size == ds.size => success(())
      case _                       => failure("Contains duplicate device IDs")
    }).disjunction

    val validations = ds.toList.map(validate).sequence.disjunction

    duplicates.flatMap { _ => validations }.validation
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

  private[this] def registerDeviceType(deviceType: PropsFactory) = {
    deviceTypes = deviceType orElse deviceTypes
  }

  private[this] def success[T](t: T): Response[T] = Validation.success(t)
  private[this] def failure[T](msg: String): Response[T] = Validation.failureNel(msg)

}

object DeviceManager {

  type PropsFactory = PartialFunction[DeviceLike, Props]

  object Protocol {
    type Response[T] = ValidationNel[String, T]
    case class Lookup(id: DeviceId)
    case class SetDevices(devices: Seq[DeviceLike])
    case class Register(props: PropsFactory)
  }

  def name = "device-manager"
  def props = Props(new DeviceManager())

}
