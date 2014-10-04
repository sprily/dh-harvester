package uk.co.sprily.dh
package harvester

import scala.concurrent.duration._

import akka.actor.ActorContext
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.testkit.TestActorRef

import org.joda.{time => joda}

import org.specs2.mutable.SpecificationLike

import network.Device
import network.DeviceId

class PollingActorSpec extends TestKit(ActorSystem("test-system"))
                          with SpecificationLike {

  override def intToRichLong(v: Int) = super.intToRichLong(v)

  "A PollingActor" should {
    "send a poll message to the device at startup" in {
      val underTest = TestActorRef(
        new PollingActor[fakeDevice.type](
          request(Every(3.seconds, grace=5.seconds)),
          fakeDeviceDirectory
        )
      ).underlyingActor

      val pollSentAt = new joda.LocalDateTime(2014, 9, 1, 13, 30, 4)
      val now        = new joda.LocalDateTime(2014, 9, 1, 13, 30, 5)

      underTest.durationUntilNextPoll(pollSentAt, now) === (Some(2.seconds))
    }
  }

  val fakeDevice = new Device {
    trait Address
    trait AddressSelection

    val id = DeviceId(1L)
    val address = new Address {}

    val selection = new AddressSelection {}
  }

  val fakeDeviceDirectory = new DeviceDirectoryService[fakeDevice.type] {
    trait Protocol extends DeviceActorProtocol[fakeDevice.type]
    val Protocol = new Protocol {}

    def lookup(device: fakeDevice.type)
              (implicit context: ActorContext) = context.actorSelection("does-not-exist")
  }

  private def request(t: Target) = PersistentRequest[fakeDevice.type](
    id = 1L,
    target = t,
    device = fakeDevice,
    selection = fakeDevice.selection)

}

