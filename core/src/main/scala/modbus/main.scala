package uk.co.sprily.dh
package harvester
package modbus

import akka.actor.ActorContext
import akka.actor.ActorSelection
import akka.actor.ActorSystem

import harvester.network._


object ModbusTest {
  def main(args: Array[String]): Unit = {

    val device = ModbusDevice(
      id=DeviceId(100L),
      address=ModbusDeviceAddress(
        deviceNumber=1,
        gateway=TCPGateway(
          address=IP4Address((127, 0, 0, 1)),
          port=5020
        )
      )
    )

    println(device)

    val directory = new ActorDirectory {
      def lookup(d: ModbusDevice)
                (implicit context: ActorContext): ActorSelection = {
        context.actorSelection("user/modbus-gw")
      }
    }

    val system = ActorSystem("all-my-actors")
    val gw = system.actorOf(
      ConnectionActor.gateway(
        device.address.gateway,
        directory),
      "modbus-gw"
    )

    import directory.Protocol._
    gw ! Poll(device, ModbusRegisterRange(50520.toShort, 4))

    Thread.sleep(10000)
    system.shutdown()

  }
}

