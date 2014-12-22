package uk.co.sprily.dh
package harvester
package capture

import scala.concurrent.duration._

import akka.actor.Props
import akka.testkit.TestActorRef

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions

import network.Device
import network.DeviceId

class DeviceActorSpec extends SpecificationLike
                         with NoTimeConversions {

  "A DeviceActor" should {

    import DeviceActor.Protocol._

    "accept new registrations" in new DeviceActorTestContext {

      val underTest = deviceActor
      underTest ! RegisterDevice { case d@AnotherFakeDevice() => ??? }
      underTest ! RegisterDevice { case d@FakeDevice(_) => props(d) }
      underTest ! RegisterDevice { case d@AnotherFakeDevice() => ??? }
      underTest ! SampleDevice(testRequest(1))

      expectMsgType[SampleDevice] must === (SampleDevice(testRequest(1)))
    }

    "only forward to the matching child" in new DeviceActorTestContext {
      val underTest = deviceActor
      underTest ! RegisterDevice { case d@AnotherFakeDevice() => ??? }
      underTest ! SampleDevice(testRequest(1))
      expectNoMsg(200.millis)
    }

  }

  class DeviceActorTestContext extends AkkaSpecs2Support {

    def deviceActor = TestActorRef(new DeviceActor())

    case class FakeDevice(id: DeviceId) extends Device {
      type Address = String
      type AddressSelection = Int
      type Measurement = List[Int]
      def address = "not-so-unique-address"
    }

    def props(d: FakeDevice): Props = Props(new ForwardingActor())

    case class FakeDeviceRequest(device: FakeDevice) extends Request2 {
      type D = FakeDevice
      val selection = 10
    }

    case class AnotherFakeDevice() extends Device {
      type Address = String
      type AddressSelection = String
      type Measurement = Nothing
      def id = DeviceId(999)
      def address = "not-used"
    }

    def testRequest(id: Long) = FakeDeviceRequest(FakeDevice(DeviceId(id)))
  }

}
