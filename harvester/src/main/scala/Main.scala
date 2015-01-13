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

import controllers._
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
    DeviceManager.props,
    DeviceManager.name)


  ModbusGatewayActor.registerWithDirectory(system)
  ModbusDeviceActor.registerWithManager(system)
  devices ! DeviceManager.Protocol.SetDevices(List(device))

  import RequestActorManager.Protocol.PersistentRequests
  import RequestActorManager.Protocol.ScheduledRequest

  val bus = new AkkaResponseBus()
  val client = Await.result(AsyncSimpleClient.connect(MqttOptions.cleanSession()), 3.seconds)

  val manager = system.actorOf(Props(
    new RequestActorManager(bus)),
    "request-manager")

  val publisher = system.actorOf(Props(
    new ResultsPublisher( Topic("test-org"), bus, client)), "mqtt-publisher")

  val apiActor = system.actorOf(mqtt.Requests.props(Topic("test-org"), client), "api-actor")

  val request = ModbusRequest(
    100L,
    device,
    ModbusRegisterRange(50520, 50524))

  manager ! PersistentRequests(List(
    ScheduledRequest(request, Schedule.each(3.seconds))))

  println("Press enter to stop")
  readLine()

  system.shutdown()
  AsyncSimpleClient.disconnect(client)
}
