package uk.co.sprily.dh
package harvester
package actors

import scala.concurrent.duration._
import scala.concurrent.Await

import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.actor.Props

import uk.co.sprily.mqtt._

import network._
import modbus._
import scheduling._
import capture._
import mqtt._


object Main extends App {

  import com.typesafe.config._

  val config = ConfigFactory.parseString("""
  akka {
    loglevel = "DEBUG"

    actor {
      debug {
        receive = off
      }
    }

  }
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

  val gateways = system.actorOf(
    GatewayActorDirectory.props,
    GatewayActorDirectory.name)

  val devices = system.actorOf(
    DeviceActorDirectory.props,
    DeviceActorDirectory.name)

  ModbusGatewayActor.registerWithDirectory(system)
  ModbusDeviceActor.registerWithDirectory(system)

  import RequestActorManager.Protocol.PersistentRequests
  import RequestActorManager.Protocol.ScheduledRequest

  val request = ModbusRequest(999, device, ModbusRegisterRange(50520, 50524))
  val schedule = Schedule.each(1.seconds).fixTimeoutTo(3.seconds)
  val bus = new AkkaResponseBus()
  val client = Await.result(AsyncSimpleClient.connect(MqttOptions.cleanSession()), 3.seconds)

  val manager = system.actorOf(Props(
    new RequestActorManager(bus)),
    "request-manager")

  val publisher = system.actorOf(Props(
    new ResultsPublisher( Topic("test-org"), bus, client)), "mqtt-publisher")

  val persistentRequests = PersistentRequests(List(
    ScheduledRequest(request, schedule)))
  manager ! persistentRequests

  //(1 to 10).foreach { i =>
  //  sender
  //  Thread.sleep(1000)
  //}

  println("Press enter to stop")
  readLine()

  system.shutdown()
  AsyncSimpleClient.disconnect(client)
}
