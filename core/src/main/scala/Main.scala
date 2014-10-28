package uk.co.sprily.dh
package harvester

import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.actor.Props

import scala.concurrent.duration._

import network._
import modbus._
import scheduling._

object Main {
  def main(args: Array[String]): Unit = {

    import com.typesafe.config._

    val config = ConfigFactory.parseString("""
    //akka {
    //  loglevel = "DEBUG"
    //}
    """).withFallback(ConfigFactory.load())

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

    val system = ActorSystem("all-my-actors", config)

    val req = PersistentRequest[ModbusDevice](
      1L,
      Schedule.each(1.seconds),
      device,
      ModbusRegisterRange(50520, 4))

    implicit val directory = new ActorDirectory {
      def lookup(d: ModbusDevice)
                (implicit context: ActorContext): ActorSelection = {
        context.actorSelection("/user/modbus-gw")
      }
    }

    implicit val bus = new AkkaDeviceBus()

    val printer = system.actorOf(Props(new Actor {
      def receive = {
        case o => println(s"RCVD: ${o}")
      }
    }))
    bus.subscribe(printer, device)

    val gw = system.actorOf(
      ConnectionActor.gateway(
        device.address.gateway,
        directory, 1),
      "modbus-gw"
    )

    val poller = system.actorOf(PollingActor.props(req))
    poller ! PollingActor.Protocol.StartActor

    Thread.sleep(60000)
    system.shutdown()
  }
}
