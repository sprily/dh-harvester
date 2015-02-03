package uk.co.sprily.dh
package harvester
package controllers

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions
import org.specs2.scalaz.ValidationMatchers

import capture.RequestLike
import network.DeviceId
import network.DeviceLike

class DeviceManagerSpec extends SpecificationLike
                           with ValidationMatchers
                           with NoTimeConversions {

//  "A DeviceManager" should {
//
//    import DeviceManager.Protocol._
//
//    "start with no Devices under its control" in new DeviceManagerContext {
//      val underTest = deviceManager
//      underTest ! Lookup(DeviceId(0L))
//
//      expectMsgType[Option[DeviceLike]] must === (None)
//    }
//
//    "accept new devices to manage" in new DeviceManagerContext {
//      val underTest = deviceManager
//      val devices = List(
//        FakeDevice(DeviceId(1L)),
//        FakeDevice(DeviceId(2L)))
//      underTest ! Register { case FakeDevice(_) => discard }
//      underTest ! SetDevices(devices)
//      underTest ! Lookup(DeviceId(1L))
//
//      (expectMsgType[Response[Seq[DeviceLike]]] must be successful(devices)) and
//      (expectMsgType[Option[DeviceLike]] must === (Some(devices(0))))
//    }
//
//    "reject new devices if the type is unsupported" in new DeviceManagerContext {
//      val underTest = deviceManager
//      val devices = List(
//        FakeDevice(DeviceId(1L)),
//        AnotherFakeDevice())  // unsupported device type
//      underTest ! Register { case FakeDevice(_) => discard }
//      underTest ! SetDevices(devices)
//
//      expectMsgType[Response[Seq[DeviceLike]]] must be failing
//    }
//
//    "forward requests to the child actor" in new DeviceManagerContext {
//      val underTest = deviceManager
//      val devices = List(
//        FakeDevice(DeviceId(1L)),
//        FakeDevice(DeviceId(2L)))
//      val request = FakeRequest(1L, devices(0))
//      underTest ! Register { case FakeDevice(_) => forward }
//      underTest ! SetDevices(devices)
//      underTest ! request
//
//      (expectMsgType[Response[Seq[DeviceLike]]] must be successful) and
//      (expectMsgType[FakeRequest] must === (request))
//    }
//
//    "not re-create the child actor if device doesn't change" in new DeviceManagerContext {
//      val underTest = deviceManager
//      val devices = List(
//        FakeDevice(DeviceId(1L)),
//        FakeDevice(DeviceId(2L)))
//      val request = FakeRequest(1L, devices(0))
//      underTest ! Register { case FakeDevice(_) => counter }
//
//      underTest ! SetDevices(devices)
//      underTest ! request
//      (expectMsgType[Response[Seq[DeviceLike]]] must be successful) and
//      (expectMsgType[Int] must === (1))
//
//      underTest ! SetDevices(devices)
//      underTest ! request
//      (expectMsgType[Response[Seq[DeviceLike]]] must be successful) and
//      (expectMsgType[Int] must === (2))
//    }
//
//    "remove devices if necessary" in new DeviceManagerContext {
//      val underTest = deviceManager
//      val devices = List(
//        FakeDevice(DeviceId(1L)),
//        FakeDevice(DeviceId(2L)))
//      underTest ! Register { case FakeDevice(_) => discard }
//
//      underTest ! SetDevices(devices)
//      underTest ! Lookup(DeviceId(1L))
//      (expectMsgType[Response[Seq[DeviceLike]]] must be successful(devices)) and
//      (expectMsgType[Option[DeviceLike]] must === (Some(devices(0))))
//
//      underTest ! SetDevices(devices.tail)
//      underTest ! Lookup(DeviceId(1L))
//      (expectMsgType[Response[Seq[DeviceLike]]] must be successful) and
//      (expectMsgType[Option[DeviceLike]] must === (None))
//    }
//
//    "remove child actor when removing device" in new DeviceManagerContext {
//      val underTest = deviceManager
//      val devices = List(
//        FakeDevice(DeviceId(1L)),
//        FakeDevice(DeviceId(2L)))
//      val request = FakeRequest(1L, devices(0))
//      underTest ! Register { case FakeDevice(_) => counter }
//
//      underTest ! SetDevices(devices)
//      underTest ! request
//      (expectMsgType[Response[Seq[DeviceLike]]] must be successful(devices)) and
//      (expectMsgType[Int] must === (1))
//
//      // remove the device
//      underTest ! SetDevices(devices.tail)
//      (expectMsgType[Response[Seq[DeviceLike]]] must be successful)
//
//      // re-instate the device again, and check the counter was reset
//      underTest ! SetDevices(devices)
//      underTest ! request
//      (expectMsgType[Response[Seq[DeviceLike]]] must be successful(devices)) and
//      (expectMsgType[Int] must === (1))
//    }
//
//    "return a failure when setting devices with duplicated ids" in new DeviceManagerContext {
//      val underTest = deviceManager
//      val devices = List(
//        FakeDevice(DeviceId(1L)),
//        FakeDevice(DeviceId(1L)))
//
//      underTest ! Register { case _ => counter }
//      underTest ! SetDevices(devices)
//      expectMsgType[Response[Seq[DeviceLike]]] must be failing
//    }
//
//  }
//
//  class DeviceManagerContext extends AkkaSpecs2Support with ImplicitSender {
//
//    def deviceManager = TestActorRef(new DeviceManager())
//
//    case class FakeDevice(id: DeviceId) extends DeviceLike {
//      type Address = String
//      type AddressSelection = Int
//      type Measurement = List[Int]
//      def address = "not-so-unique-address"
//    }
//
//    case class AnotherFakeDevice() extends DeviceLike {
//      type Address = String
//      type AddressSelection = String
//      type Measurement = Nothing
//      def id = DeviceId(999)
//      def address = "not-used"
//    }
//
//    case class FakeRequest(
//          id: Long,
//          device: FakeDevice) extends RequestLike {
//      type Device = FakeDevice
//      val selection = 101
//    }
//
//    def discard = Props(new DiscardingActor())
//    def forward = Props(new ForwardingActor())
//    def reply = Props(new EchoActor())
//    def counter = Props(new Actor {
//      private var count = 0
//      def receive = {
//        case _       => count += 1 ; sender ! count
//      }
//    })
//
//  }

}
