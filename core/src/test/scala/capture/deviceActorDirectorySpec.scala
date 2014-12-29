package uk.co.sprily.dh
package harvester
package capture

import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.actor.Props
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions

import network.Device
import network.DeviceId

class DeviceActorDirectorySpec extends SpecificationLike
                                  with NoTimeConversions {

  "A DeviceActorDirectory" should {

    import DeviceActorDirectory.Protocol._

    "accept new registrations" in new DeviceActorTestContext {

      val underTest = deviceActor
      underTest ! Register { case d@AnotherFakeDevice() => ??? }
      underTest ! Register { case d@FakeDevice(_)       => discard }
      underTest ! Register { case d@AnotherFakeDevice() => ??? }
      underTest ! Lookup(FakeDevice(DeviceId(1)))

      expectMsgType[ActorRef]
    }

    "only lookup the matching child" in new DeviceActorTestContext {
      val underTest = deviceActor
      underTest ! Register { case d@AnotherFakeDevice() => ??? }
      underTest ! Lookup(FakeDevice(DeviceId(1)))
      expectNoMsg(200.millis)
    }

    "forward to the matching child" in new DeviceActorTestContext {
      val underTest = deviceActor
      underTest ! Register { case d@FakeDevice(_) => reply }
      underTest ! Forward(FakeDevice(DeviceId(1)), "a message")

      expectMsgType[String] must === ("a message")
    }

  }

  class DeviceActorTestContext extends AkkaSpecs2Support with ImplicitSender {

    def deviceActor = TestActorRef(new DeviceActorDirectory())

    case class FakeDevice(id: DeviceId) extends Device {
      type Address = String
      type AddressSelection = Int
      type Measurement = List[Int]
      def address = "not-so-unique-address"
    }

    def discard = Props(new DiscardingActor())
    def forward = Props(new ForwardingActor())
    def reply = Props(new EchoActor())

    case class AnotherFakeDevice() extends Device {
      type Address = String
      type AddressSelection = String
      type Measurement = Nothing
      def id = DeviceId(999)
      def address = "not-used"
    }

  }

}
