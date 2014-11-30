package uk.co.sprily.dh
package harvester
package capture

import scala.language.reflectiveCalls

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.ActorSelection
import akka.actor.Props
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.util.ByteString

import org.joda.time.LocalDateTime

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions

import modbus._
import network._
import scheduling._

class DeviceManagerActorSpec extends SpecificationLike
                                with NoTimeConversions {

  import DeviceManagerActor.Protocol._

  "A DeviceManagerActor" should {

    "Make adhoc requests to a given device" in new DMTestContext() {
      val underTest = manager

      val expectedPoll = directory.Protocol.Poll(
        req1.device,
        req1.selection)

      underTest ! ModbusRequest(req1)
      expectMsgType[Poll] must === (expectedPoll)
    }

    "Make persistent requests to given devices" in new DMTestContext() {
      val underTest = manager

      val exp1 = directory.Protocol.Poll(
        req1.device, req1.selection)

      val exp2 = directory.Protocol.Poll(
        req2.device, req2.selection)

      val pReq = PersistentRequests(List(
        ModbusRequest(req1),
        ModbusRequest(req2)))

      underTest ! pReq
      val poll1 = expectMsgType[Poll]
      val poll2 = expectMsgType[Poll]

      Set(poll1, poll2) must === (Set(exp1, exp2))
    }

    "Remove requests which have completed" in new DMTestContext() {
      val underTest = manager
      val poll = directory.Protocol.Poll(
        req1.device, req1.selection)

      underTest ! ModbusRequest(req1)
      val poll1 = expectMsgType[Poll]

      Thread.sleep(300)

      underTest ! ModbusRequest(req1)
      val poll2 = expectMsgType[Poll]

      (poll1 must === (poll)) and
      (poll2 must === (poll))
    }

    "Update persistent requests" in new DMTestContext() {
      val underTest = manager
      
      val pReq1 = PersistentRequests(List(
        ModbusRequest(req1),
        ModbusRequest(req2)))

      // send the initial request
      underTest ! pReq1
      expectMsgType[Poll]
      expectMsgType[Poll]
      Thread.sleep(300)

      // update the persistent request
      val pReq2 = PersistentRequests(List(ModbusRequest(req1)))
      underTest ! pReq2
      expectMsgType[Poll]
      expectNoMsg(1.second)
    }

  }

}

class DMTestContext extends AkkaSpecs2Support {

  type Poll = directory.Protocol.Poll
  type Result = directory.Protocol.Result
  
  lazy val fakeDeviceBus = new DeviceBus {

    var readings = List[Reading[Device]]()

    def publish[D <: Device](r: Reading[D]) = {
      readings = r.asInstanceOf[Reading[Device]] :: readings
    }

    def subscribe[D <: Device](subscriber: ActorRef, device: D): Boolean = ???
    def unsubscribe[D <: Device](subscriber: ActorRef, from: D): Boolean = ???
    def unsubscribe(subscriber: ActorRef): Unit = ???
  }

  lazy val fakeGateway = system.actorOf(Props(new Actor {
    def receive = {
      case m =>
        sender ! fakeResult
        testActor ! m
    }
  }))

  lazy val fakeResult = directory.Protocol.Result(
    LocalDateTime.now(),
    ModbusMeasurement(
      ModbusRegisterRange(1,100),
      ByteString.empty))

  lazy val directory: DeviceActorDirectory[ModbusDevice] = new DeviceActorDirectory[ModbusDevice] {
    def lookup(d: ModbusDevice) = {
      system.actorSelection(fakeGateway.path)
    }
  }

  lazy val fakeProvider = new DirectoryProvider {
    override val modbus = directory
  }

  lazy val device = ModbusDevice(
    DeviceId(10),
    ModbusDeviceAddress(
      1,
      TCPGateway(IP4Address.localhost, 5020)))

  lazy val req1 = Request[ModbusDevice](
    id = 1L,
    schedule = Schedule.single(5.seconds),
    device = device,
    selection = ModbusRegisterRange(1,10))

  lazy val req2 = Request[ModbusDevice](
    id = 2L,
    schedule = Schedule.single(5.seconds).delayBy(5.millis),
    device = device,
    selection = ModbusRegisterRange(11,20))

  def manager = TestActorRef(
    new DeviceManagerActor(fakeProvider, fakeDeviceBus))

}
