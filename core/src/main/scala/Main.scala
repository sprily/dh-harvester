package uk.co.sprily.dh
package harvester

import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.actor.Props

import scala.concurrent.duration._
import scala.concurrent.Await

import uk.co.sprily.mqtt._

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
      Schedule.each(300.millis).fixTimeoutTo(1.seconds),
      device,
      ModbusRegisterRange(50520, 4))

    val req2 = Request[ModbusDevice](
      2L,
      Schedule.each(300.millis).delayBy(150.millis).fixTimeoutTo(2.seconds),
      device,
      ModbusRegisterRange(50522, 4))

    val bus = new AkkaDeviceBus()
    val provider = new DefaultDirectoryProvider(system)
    val manager = system.actorOf(Props(
      new DeviceManagerActor(provider, bus)), "device-manager")

    val client = Await.result(AsyncSimpleClient.connect(MqttOptions.cleanSession()), 3.seconds)
    val printer = system.actorOf(mqtt.ResultsPublisher.props[ModbusDevice,Cont,AsyncSimpleClient.type]("test-org", device, bus, AsyncSimpleClient)(client))

    manager ! PersistentRequests(List(
      ModbusRequest(req1),
      ModbusRequest(req2)))

    println("Press enter to stop")
    readLine()

    system.shutdown()
    AsyncSimpleClient.disconnect(client)
  }
}
