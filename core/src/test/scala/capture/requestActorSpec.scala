package uk.co.sprily.dh
package harvester
package capture

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

import network.Device
import network.DeviceId

import scheduling.Instant
import scheduling.Schedule
import scheduling.TargetLike

class RequestActorSpec extends SpecificationLike
                          with NoTimeConversions {

  "A RequestActor" should {

    "Poll the device when started" in new TestContext {
      setupFakeDeviceActor()
      val underTest = requestActor

      expectMsgType[Request] must === (request)
    }

    "Send results to the results bus" in new TestContext {
      setupFakeDeviceActor()
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

  case class FakeDevice(id: DeviceId) extends Device {
    type Address = String
    type AddressSelection = String
    type Measurement = String

    val address = "address"
  }

  case class FakeRequest(id: Long, device: FakeDevice) extends Request {
    type D = FakeDevice
    val selection = "selection"
  }

  case class FakeResponse(m: String, device: FakeDevice) extends Response {
    type D = FakeDevice
    val measurement = m
    val timestamp = dt
  }

  def fakeDevice = FakeDevice(DeviceId(100))
  def schedule = Schedule.single(3.seconds)
  def request = FakeRequest(1, fakeDevice)
  def fakeResponse(m: String) = FakeResponse(m, fakeDevice)

  lazy val fakeBus = new ResponseBus {
    var responses = List[Response]()
    def publish(r: Response) = { responses = r :: responses }
    def subscribe(subscriber: ActorRef): Boolean = ???
    def unsubscribe(subscriber: ActorRef): Boolean = ???
  }

  def requestActor = TestActorRef(new RequestActor(request, schedule, fakeBus))

  def setupFakeDeviceActor(): Unit = {
    val directory = system.actorOf(DeviceActorDirectory.props, DeviceActorDirectory.name)
    directory ! DeviceActorDirectory.Protocol.Register {
      case _ => Props(new ForwardingActor())
    }
  }
}
