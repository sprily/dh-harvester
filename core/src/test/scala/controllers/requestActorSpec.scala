package uk.co.sprily.dh
package harvester
package controllers

import scala.language.reflectiveCalls

import scala.concurrent.duration._

import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.ActorSelection
import akka.actor.Props
import akka.testkit.TestActorRef
import akka.testkit.TestKit

import org.joda.time.LocalDateTime

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions

import network.DeviceLike
import network.DeviceId

import capture.ResponseLike
import capture.RequestLike
import scheduling.Instant
import scheduling.Schedule
import scheduling.TargetLike

class RequestActorSpec extends SpecificationLike
                          with NoTimeConversions {

  "A RequestActor" should {

    "Poll the device when started" in new TestContext {
      val underTest = requestActor

      expectMsgType[RequestLike] must === (request)
    }

    "Send results to the results bus" in new TestContext {
      val measurement = "measurement"
      val underTest = requestActor
      val response = fakeResponse(measurement)
      underTest ! response

      fakeBus.responses must === (List(response))
    }

    "Throw an exception when a timeout is hit" in new TestContext {
      (pending)
    }

    "Stop if the Schedule completes" in new TestContext {
      (pending)
    }
  }

}

class TestContext extends AkkaSpecs2Support {

  lazy val basetime = Instant.now()
  lazy val dt = LocalDateTime.now()
  lazy val fakeBus = new FakeResponseBus()
  lazy val deviceManager = {
    val manager = system.actorOf(DeviceManager.props, DeviceManager.name)
    manager ! DeviceManager.Protocol.Register {
      case _ => Props(new ForwardingActor())
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

  case class FakeResponse(m: String, device: FakeDevice) extends ResponseLike {
    type Device = FakeDevice
    val measurement = m
    val timestamp = dt
  }

  def fakeDevice = FakeDevice(DeviceId(100))
  def schedule = Schedule.single(3.seconds)
  def request = FakeRequest(1, fakeDevice)
  def fakeResponse(m: String) = FakeResponse(m, fakeDevice)
  def requestActor = TestActorRef(new RequestActor(request, schedule, fakeBus, deviceManager))
}
