package uk.co.sprily.dh
package harvester
package controllers

import scala.concurrent.duration._

import scalaz._
import Scalaz._

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions
import org.specs2.scalaz.ValidationMatchers

import network.DeviceId
import network.DeviceLike
import capture.RequestLike
import scheduling.Schedule

class InstanceManagerSpec extends SpecificationLike
                             with ValidationMatchers
                             with NoTimeConversions {

  import InstanceManager.Protocol._

  "An InstanceManager" should {
    
    "send devices to the DeviceManager" in new Context {

      val underTest = TestActorRef(new DefaultInstanceManager {
        override def deviceMgrProps = Props(new ForwardingActor())
      })

      underTest ! InstanceConfig(List(
        FakeRequest(1, FakeDevice(1)),
        FakeRequest(2, FakeDevice(1)),
        FakeRequest(3, FakeDevice(2))
      ))

      val expected = List(FakeDevice(1), FakeDevice(2))
      expectMsgType[Seq[DeviceLike]] must === (expected)
    }

    "send requests to the RequestActorManager" in new Context {

      val underTest = TestActorRef(new DefaultInstanceManager {
        override def requestMgrProps(b: ResponseBus, d: ActorRef) = {
          Props(new ForwardingActor())
        }
      })

      val requests = List(
        FakeRequest(1, FakeDevice(1)),
        FakeRequest(2, FakeDevice(1)),
        FakeRequest(3, FakeDevice(2))
      )
      underTest ! InstanceConfig(requests)

      expectMsgType[Seq[RequestLike]] must === (requests)
    }

  }

  class Context extends AkkaSpecs2Support with ImplicitSender {

    def fakeResponseBus = new FakeResponseBus()

    class DefaultInstanceManager extends InstanceManager(fakeResponseBus) with ManagerProvider {
      override def deviceMgrProps = Props(new DiscardingActor())
      override def requestMgrProps(b: ResponseBus, d: ActorRef) = {
        Props(new DiscardingActor())
      }
    }

    case class FakeDevice(_id: Long) extends DeviceLike {
      type Address = String
      type AddressSelection = Any
      type Measurement = Any
      def id = DeviceId(_id)
      def address = "address-string"
    }

    case class FakeRequest(id: Long, device: FakeDevice) extends RequestLike {
      type Device = FakeDevice
      val selection = ()
    }

  }
}
