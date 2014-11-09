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
import capture._

import DeviceManagerActor.Protocol._

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

    val req1 = Request[ModbusDevice](
      1L,
      Schedule.each(5.seconds),
      device,
      ModbusRegisterRange(50520, 4))

    val req2 = Request[ModbusDevice](
      2L,
      Schedule.each(3.seconds).take(2).fixTimeoutTo(1.millis),
      device,
      ModbusRegisterRange(50524, 4))

    val bus = new AkkaDeviceBus()
    val provider = new DefaultDirectoryProvider(system)
    val manager = system.actorOf(Props(
      new DeviceManagerActor(provider, bus)), "device-manager")

    val printer = system.actorOf(Props(new Actor {
      def receive = {
        case o => println(s"RCVD: ${o}")
      }
    }), "printer")
    bus.subscribe(printer, device)

    manager ! PersistentRequests(List(
      ModbusRequest(req1),
      ModbusRequest(req2)))

    Thread.sleep(16000)

    manager ! ModbusRequest(req2)

    Thread.sleep(7000)
    system.shutdown()
  }
}
