package uk.co.sprily.dh
package harvester
package modbus

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.routing.RoundRobinPool
import akka.util.ByteString

import capture.GatewayActorDirectory
import controllers.DeviceManager

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

  def registerWithManager(system: ActorSystem): Unit = {
    import DeviceManager.Protocol.Register
    val manager = system.actorSelection(s"/user/${DeviceManager.name}")
    manager ! Register {
      case (d: ModbusDevice) => props(d)
    }
  }

  def props(d: ModbusDevice) = Props(new ModbusDeviceActor(d))

}

