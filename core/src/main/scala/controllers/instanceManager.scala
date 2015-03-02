package uk.co.sprily.dh
package harvester
package controllers

import scala.concurrent.duration.FiniteDuration

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props

import capture.ScheduledRequestLike
import network.DeviceId
import scheduling.Schedule

protected[controllers] trait DeviceManagerProvider {
  def deviceMgrProps: Props
}

protected[controllers] trait RequestManagerProvider {
  def requestMgrProps(bus: ResponseBus, deviceMgr: ActorRef): Props
}

protected[controllers] trait ManagerProvider extends DeviceManagerProvider
                                                with RequestManagerProvider

class InstanceManager(bus: ResponseBus) extends Actor
                                           with ActorLogging {
  this: ManagerProvider =>

  import InstanceManager.Protocol._
  import DeviceManager.Protocol._
  import RequestManager.Protocol._

  /** The following are initialised with every (re)start **/
  private[this] var deviceMgr: ActorRef = _
  private[this] var reqMgr:    ActorRef = _

  /** Actor Hooks **/
  override def preStart(): Unit = {
    log.info("Creating device manager")
    deviceMgr = context.actorOf(deviceMgrProps, "device-manager")
    modbus.ModbusDeviceActor.registerWithManager(deviceMgr)

    log.info("Creating request manager")
    reqMgr    = context.actorOf(requestMgrProps(bus, deviceMgr), "request-manager")
  }

  def receive = {
    case (c: InstanceConfig)      => setConfig(c)
    case other                    => deviceMgr forward other
  }

  private[this] def setConfig(c: InstanceConfig) = {
    log.info(s"InstanceManager setting config: $c")
    deviceMgr ! SetDevices(c.devices)
    log.info(s"Sending ${c.requests} to $reqMgr")
    reqMgr ! PersistentRequests(c.requests)
    sender ! Acked  // TODO: really, should be waiting from positive response from both
  }

}

object InstanceManager {

  def name = "instance-manager"
  def props(bus: ResponseBus) = Props(new InstanceManager(bus) with ManagerProvider {
    override def deviceMgrProps = DeviceManager.props
    override def requestMgrProps(bus: ResponseBus, deviceMgr: ActorRef) = {
      RequestManager.props(bus, deviceMgr)
    }
  })

  object Protocol {
    case class InstanceConfig(requests: Seq[ScheduledRequestLike]) {
      def devices = requests.map(_._1.device).distinct
    }
    case object Acked
  }

}
