package uk.co.sprily.dh
package harvester
package modbus

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._
import akka.routing.RoundRobinPool
import akka.util.ByteString

import capture.DeviceActorDirectory
import capture.GatewayActorDirectory

/** An Actor representing a single ModbusDevice.
  *
  * It processes `ModbusRequest` tasks by forwarding the request to the
  * appropriate gateway (through a `GatewayActorDirectory` service actor).
  */
class ModbusDeviceActor(
    device: ModbusDevice) extends Actor
                             with ActorLogging {

  import GatewayActorDirectory.Protocol.Forward

  private lazy val gateway = context.actorSelection(s"/user/${GatewayActorDirectory.name}")

  def receive = {
    case req@ModbusRequest(_, device, _) =>
      gateway forward Forward(device.address.gateway, req)
  }

}

object ModbusDeviceActor {

  import DeviceActorDirectory.Protocol.Register

  def registerWithDirectory(system: ActorSystem): Unit = {
    val directory = system.actorSelection(s"/user/${DeviceActorDirectory.name}")
    directory ! Register {
      case (d: ModbusDevice) => props(d)
    }
  }

  def props(d: ModbusDevice) = Props(new ModbusDeviceActor(d))

}

