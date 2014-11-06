package uk.co.sprily.dh
package harvester
package capture

import scala.language.reflectiveCalls

import scala.concurrent.duration._

import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.ActorSelection
import akka.testkit.TestActorRef
import akka.testkit.TestKit

import org.joda.time.LocalDateTime

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions

import network.Device
import network.DeviceId
import scheduling.Schedule

class PollingActorSpec extends SpecificationLike
                          with NoTimeConversions {

  "A PollingActor" should {

    "Poll the device" in new PollingActorContext {

      val underTest = pollingActor
      underTest ! PollingActor.Protocol.StartActor
      underTest ! PollingActor.Protocol.PollNow

      expectMsgType[Poll] must === (pollMsg)
    }

    "Send results to <somewhere>" in new PollingActorContext {
      val measurement: fakeDevice.Measurement = List((1,100), (2,200))
      val underTest = pollingActor
      underTest ! deviceDirectory.Protocol.Result(dt, measurement)

      val expected = List(Reading[fakeDevice.type](
        dt, fakeDevice, measurement))
      
      fakeDeviceBus.readings must === (expected)
    }

    "Throw an exception when a timeout is hit" in new PollingActorContext {
      (pending)
    }
  }

}

class PollingActorContext extends AkkaSpecs2Support {

  // for brevity
  type TestDevice = fakeDevice.type
  type Poll = deviceDirectory.Protocol.Poll

  lazy val dt = LocalDateTime.now()

  lazy val fakeDevice = new Device {
    trait Address
    trait AddressSelection
    type Measurement = List[(Int,Int)]

    val id = DeviceId(1L)
    val address = new Address {}
    val selection = new AddressSelection {}
  }

  def request(s: Schedule) = PersistentRequest[TestDevice](
    id = 1L,
    schedule = s,
    device = fakeDevice,
    selection = fakeDevice.selection)

  lazy val deviceDirectory = new DeviceActorDirectory[TestDevice] {
    def lookup(device: TestDevice) = {
      system.actorSelection(testActor.path)
    }
  }

  lazy val pollMsg = deviceDirectory.Protocol.Poll(
    fakeDevice,
    fakeDevice.selection
  )

  lazy val fakeDeviceBus = new DeviceBus {

    var readings = List[Reading[Device]]()

    def publish[D <: Device](r: Reading[D]) = {
      readings = r.asInstanceOf[Reading[Device]] :: readings
    }

    def subscribe[D <: Device](subscriber: ActorRef, device: D): Boolean = ???
    def unsubscribe[D <: Device](subscriber: ActorRef, from: D): Boolean = ???
    def unsubscribe(subscriber: ActorRef): Unit = ???
  }

  def pollingActor = TestActorRef(
    new PollingActor[fakeDevice.type](
      request(Schedule.each(3.seconds)),
      deviceDirectory,
      fakeDeviceBus
    )
  )

}
