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

import controllers._
import modbus._
import network._
import scheduling._

class RequestActorManagerSpec extends SpecificationLike
                                 with NoTimeConversions {

  import RequestActorManager.Protocol._

  "A RequestActorManager" should {

    "Make adhoc requests to a given device" in new TestContext() {
      val underTest = manager
      underTest ! adhocRequest
      expectMsgType[RequestLike] must === (adhocRequest)
    }

    "Ignore ScheduledRequests to a given device" in new TestContext() {
      val underTest = manager
      underTest ! ScheduledRequest(adhocRequest, Schedule.single(3.seconds))
      expectNoMsg(300.millis)
    }

    "Make persistent requests to given devices" in new TestContext() {
      val underTest = manager
      underTest ! persistentRequests

      val req1 = expectMsgType[RequestLike]
      val req2 = expectMsgType[RequestLike]
      Set(req1, req2) must === (persistentRequests.requests.map(_.request).toSet)
    }

    "Remove requests which have completed" in new TestContext() {
      val underTest = manager

      underTest ! adhocRequest
      val req1 = expectMsgType[RequestLike]

      underTest ! adhocRequest
      val req2 = expectMsgType[RequestLike]

      (req1 must === (adhocRequest)) and
      (req2 must === (adhocRequest))
    }

    "Handle duplicated adhoc requests" in new TestContext () {
      val underTest = logExceptions(managerProps)
      underTest ! adhocRequest
      underTest ! adhocRequest  // same request id "actor name [10] is not unique!"
      expectMsgType[RequestLike]
      expectNoMsg
    }

    "Update persistent requests" in new TestContext() {
      val underTest = manager
      
      // send the initial request
      underTest ! persistentRequests
      expectMsgType[RequestLike]
      expectMsgType[RequestLike]

      // update the persistent request
      val newReqs = PersistentRequests(persistentRequests.requests.tail)
      underTest ! newReqs
      expectMsgType[RequestLike]
      expectNoMsg(300.millis)
    }

  }

  class TestContext extends AkkaSpecs2Support {

    lazy val dt = LocalDateTime.now()
    lazy val fakeBus = new FakeResponseBus()
    lazy val fakeDevice = FakeDevice(DeviceId(100))
    lazy val deviceManager = {
      val manager = system.actorOf(DeviceManager.props)
      manager ! DeviceManager.Protocol.Register {
        case _ => Props(new ForwardingActor(
          respondWith = { case _ => Some(FakeResponse) }
        ))
      }
      manager ! DeviceManager.Protocol.SetDevices(List(fakeDevice))
      manager
    }
   
    case class FakeDevice(id: DeviceId) extends DeviceLike {
      type Address = String
      type AddressSelection = String
      type Measurement = String
      val address = "address"
    }

    case class FakeRequest(id: Long, device: FakeDevice) extends RequestLike {
      type Device = FakeDevice
      val selection = "selection"
    }

    case object FakeResponse extends ResponseLike {
      type Device = FakeDevice
      val device = fakeDevice
      val measurement = "measurement"
      lazy val timestamp = dt
    }

    def adhocRequest = FakeRequest(10, fakeDevice)

    def persistentRequests = PersistentRequests(List(
      ScheduledRequest(adhocRequest, Schedule.each(3.seconds)),
      ScheduledRequest(adhocRequest.copy(id=11), Schedule.each(2.seconds))
    ))

    def manager = TestActorRef(managerProps)
    def managerProps = Props(new RequestActorManager(fakeBus, deviceManager))
  }
}

