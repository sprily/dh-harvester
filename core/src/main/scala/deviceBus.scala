package uk.co.sprily.dh
package harvester

import akka.actor.ActorRef
import akka.event.EventBus
import akka.event.LookupClassification

import network.Device

/** Transmits changes to the connected devices **/
trait DeviceBus {
  import DeviceBus.Protocol
  def publish(event: Protocol.Event): Unit
  def subscribe(subscriber: ActorRef): Boolean
  def unsubscribe(subscriber: ActorRef): Boolean
}

object DeviceBus {
  object Protocol {
    sealed trait Event
    case class DeviceAdded(device: Device) extends Event
    case class DeviceRemoved(device: Device) extends Event
  }
}

/** Implementation based on an Akka EventBus **/
class AkkaDeviceBus extends DeviceBus {

  import DeviceBus.Protocol
  private case object CatchAll

  private val underlying = new EventBus with LookupClassification {
    type Event = Protocol.Event
    type Classifier = CatchAll.type
    type Subscriber = ActorRef

    def classify(e: Event) = CatchAll
    def publish(e: Event, a: ActorRef) = a ! e
    def compareSubscribers(a1: ActorRef, a2: ActorRef) = a1.compareTo(a2)
    def mapSize = 1
  }

  def publish(event: Protocol.Event) = underlying.publish(event)
  def subscribe(subscriber: ActorRef) = underlying.subscribe(subscriber, CatchAll)
  def unsubscribe(subscriber: ActorRef) = underlying.unsubscribe(subscriber, CatchAll)

}
