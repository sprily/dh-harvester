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

import api._
import controllers._
import network._
import modbus._
import capture._
import mqtt._
import scheduling.Schedule


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
  val bus = new AkkaResponseBus()

  val gateways = system.actorOf(
    GatewayActorDirectory.props,
    GatewayActorDirectory.name)

  val instanceManager = system.actorOf(
    InstanceManager.props(bus),
    InstanceManager.name)

  instanceManager ! InstanceManager.Protocol.InstanceConfig(Nil)

  ModbusGatewayActor.registerWithDirectory(system)
  //ModbusDeviceActor.registerWithManager(system)

  import RequestManager.Protocol.PersistentRequests

  val client = Await.result(AsyncSimpleClient.connect(MqttOptions.cleanSession()), 3.seconds)

  val publisher = system.actorOf(Props(
    new ResultsPublisher( Topic("test-org"), bus, client)), "mqtt-publisher")

  val api = system.actorOf(
    InstanceConfigEndpoint.props(Topic("test-org/instance-config"),
                                 client, instanceManager,
                                 timeout=10.seconds),
    "api-instance-config")

  val adhocRequestEndpoint = system.actorOf(
    AdhocRequestsEndpoint.props(Topic("test-org/adhoc-requests"),
                                client, instanceManager,
                                timeout=10.seconds),
    "api-adhoc-requests")

  val request = ModbusRequest(
    device,
    ModbusRegisterRange(50520, 50524))

  val scheduledRequest = (request, Schedule.each(5.seconds))

  instanceManager ! InstanceManager.Protocol.InstanceConfig(List(scheduledRequest))

  println("Press enter to stop")
  readLine()

  system.shutdown()
  AsyncSimpleClient.disconnect(client)
}
