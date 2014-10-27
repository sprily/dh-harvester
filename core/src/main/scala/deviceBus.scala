package uk.co.sprily.dh
package harvester

import network.Device

import akka.actor.ActorRef
import akka.event.EventBus
import akka.event.LookupClassification

trait DeviceBus {
  def publish[D <: Device](reading: Reading[D]): Unit
  def subscribe[D <: Device](subscriber: ActorRef, device: D): Boolean
  def unsubscribe[D <: Device](subscriber: ActorRef, from: D): Boolean
  def unsubscribe(subscriber: ActorRef): Unit
}

/** Implementation based on Akka's EventBus.
  *
  * Going through Akka's EventBus means we lose track of the
  * types.  It's not such a big issue, as on the way in, we can
  * safely up-cast.  And on the way out, we're dealing with
  * actors anyway (ie AnyRef => Unit).
  */
class AkkaDeviceBus extends DeviceBus {

  def publish[D <: Device](reading: Reading[D]): Unit = {
    underlying.publish(reading.asInstanceOf[Reading[Device]])
  }

  def subscribe[D <: Device](subscriber: ActorRef, device: D) = {
    underlying.subscribe(subscriber, device)
  }

  def unsubscribe[D <: Device](subscriber: ActorRef, from: D) = {
    underlying.unsubscribe(subscriber, from)
  }

  def unsubscribe(subscriber: ActorRef) = {
    underlying.unsubscribe(subscriber)
  }

  val underlying = new EventBus with LookupClassification {
    type Event = Reading[Device]
    type Classifier = Device
    type Subscriber = ActorRef
     
    // is used for extracting the classifier from the incoming events
    override protected def classify(event: Event): Classifier = event.device
     
    // will be invoked for each event for all subscribers which registered themselves
    // for the event’s classifier
    override protected def publish(event: Event, subscriber: Subscriber): Unit = {
      subscriber ! event
    }
     
    // must define a full order over the subscribers, expressed as expected from
    // `java.lang.Comparable.compare`
    override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = {
      a.compareTo(b)
    }
     
    // determines the initial size of the index data structure
    // used internally (i.e. the expected number of different classifiers)
    override protected def mapSize: Int = 16
  }
}
