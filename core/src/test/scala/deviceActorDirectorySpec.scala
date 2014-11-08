package uk.co.sprily.dh
package harvester

import akka.actor.Actor
import akka.actor.Props
import akka.testkit.ImplicitSender

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions

import network.Device
import network.DeviceId

class DeviceActorDirectorySpec extends SpecificationLike
                                  with NoTimeConversions {

  "A DeviceActorDirectoryImpl" should {

    "Create new device actors as necessary" in new DirectoryContext {
      val underTest = directory(echoActorProps)

      import underTest.Protocol.Poll
      val poll = Poll(fakeDevice, List("/sensor1", "/sensor2"))
      val ref = underTest.lookup(fakeDevice)
      ref ! poll

      expectMsgType[Poll] must === (poll)
    }

    "Recreate failed child actors" in new DirectoryContext {
      import java.util.concurrent.atomic.AtomicInteger
      val createdCounter = new AtomicInteger(0)
      val underTest = directory(Props(new Actor {

        var msgCounter = 0

        override def preStart(): Unit = {
          createdCounter.incrementAndGet()
          msgCounter = 0
        }

        def receive = {
          case m =>
            msgCounter += 1
            sender ! createdCounter.get()
            if (msgCounter >= 2) throw new Exception("Oops")
        }
      }))

      import underTest.Protocol.Poll
      val poll = Poll(fakeDevice, List("/sensor1", "/sensor2"))
      val ref = underTest.lookup(fakeDevice)

      ref ! poll
      expectMsgType[Int] must === (1)

      ref ! poll
      expectMsgType[Int] must === (1)

      ref ! poll
      expectMsgType[Int] must === (2)
    }

  }

}

class DirectoryContext extends AkkaSpecs2Support with ImplicitSender {

  case object Response


  def directory(props: => Props) = {
    new DeviceActorDirectoryImpl[FakeDevice](system) {
      type NetLoc = String
      def netLocFor(d: FakeDevice) = d.address
      def workerProps(netLoc: NetLoc) = props
      def directoryName = "fake"
      def actorPathFor(loc: NetLoc) = loc
    }
  }

  def echoActorProps = Props(new Actor {
    def receive = {
      case m => sender ! m
    }
  })

  case class FakeDevice(
      val id: DeviceId,
      val address: String) extends Device {
    type Address = String
    type AddressSelection = List[String]
    type Measurement = Double
  }

  def fakeDevice = FakeDevice(DeviceId(10), "device-1")
}
