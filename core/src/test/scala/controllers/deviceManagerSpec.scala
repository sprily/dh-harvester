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

  "A DeviceManager" should {

    import DeviceManager.Protocol._

    "start with no Devices under its control" in new DeviceManagerContext {
      val underTest = deviceManager
      val d = FakeDevice(DeviceId(1))
      underTest ! FakeRequest(1L, d)
      expectMsgType[UnknownDevice] must === (UnknownDevice(d))
    }

    "accept new devices to manage" in new DeviceManagerContext {
      val underTest = deviceManager
      val devices = List(
        FakeDevice(DeviceId(1L)),
        FakeDevice(DeviceId(2L)))
      val request = FakeRequest(1L, devices(0))
      underTest ! Register { case FakeDevice(_) => forward }
      underTest ! SetDevices(devices)
      underTest ! request

      expectMsgType[FakeRequest] must === (request)
    }

    "reject new devices if the type is unsupported" in new DeviceManagerContext {
      val underTest = deviceManager
      val devices: List[DeviceLike] = List(
        FakeDevice(DeviceId(1L)),
        AnotherFakeDevice())  // unsupported device type
      val request = AnotherFakeRequest(1L, AnotherFakeDevice())
      underTest ! Register { case FakeDevice(_) => discard }
      underTest ! SetDevices(devices)
      underTest ! request

      expectMsgType[UnknownDevice] must === (UnknownDevice(devices(1)))
    }

    "not re-create the child actor if device doesn't change" in new DeviceManagerContext {
      val underTest = deviceManager
      val devices = List(
        FakeDevice(DeviceId(1L)),
        FakeDevice(DeviceId(2L)))
      val request = FakeRequest(1L, devices(0))
      underTest ! Register { case FakeDevice(_) => counter }

      underTest ! SetDevices(devices)
      underTest ! request
      expectMsgType[Int] must === (1)   // from the counter actor

      underTest ! SetDevices(devices)
      underTest ! request
      expectMsgType[Int] must === (2)   // counter not reset
    }

    "remove devices if necessary" in new DeviceManagerContext {
      val underTest = deviceManager
      val devices = List(
        FakeDevice(DeviceId(1L)),
        FakeDevice(DeviceId(2L)))
      val request = FakeRequest(1, devices(0))
      underTest ! Register { case FakeDevice(_) => forward }

      underTest ! SetDevices(devices)
      underTest ! request
      expectMsgType[FakeRequest] must === (request)

      underTest ! SetDevices(devices.tail)
      underTest ! request
      expectMsgType[UnknownDevice] must === (UnknownDevice(devices(0)))
    }

    "remove child actor when removing device" in new DeviceManagerContext {
      val underTest = deviceManager
      val devices = List(
        FakeDevice(DeviceId(1L)),
        FakeDevice(DeviceId(2L)))
      val request = FakeRequest(1L, devices(0))
      underTest ! Register { case FakeDevice(_) => counter }

      underTest ! SetDevices(devices)
      underTest ! request
      expectMsgType[Int] must === (1)

      // remove the device
      underTest ! SetDevices(devices.tail)

      // re-instate the device again, and check the counter was reset
      underTest ! SetDevices(devices)
      underTest ! request
      expectMsgType[Int] must === (1)
    }

  }

  class DeviceManagerContext extends AkkaSpecs2Support with ImplicitSender {

    def deviceManager = TestActorRef(new DeviceManager())

    case class FakeDevice(id: DeviceId) extends DeviceLike {
      type Address = String
      type AddressSelection = Int
      type Measurement = List[Int]
      def address = "not-so-unique-address"
    }

    case class AnotherFakeDevice() extends DeviceLike {
      type Address = String
      type AddressSelection = String
      type Measurement = Nothing
      def id = DeviceId(999)
      def address = "not-used"
    }

    case class FakeRequest(
          id: Long,
          device: FakeDevice) extends RequestLike {
      type Device = FakeDevice
      val selection = 101
    }

    case class AnotherFakeRequest(id: Long, device: AnotherFakeDevice) extends RequestLike {
      type Device = AnotherFakeDevice
      val selection = "102"
    }

    def discard = Props(new DiscardingActor())
    def forward = Props(new ForwardingActor())
    def reply = Props(new EchoActor())
    def counter = Props(new Actor {
      private var count = 0
      def receive = {
        case _       => count += 1 ; sender ! count
      }
    })

  }

}
