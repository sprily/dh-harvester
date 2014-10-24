package uk.co.sprily.dh
package harvester

import network.Device
import network.Reading

import akka.actor.ActorRef
import akka.event.EventBus
import akka.event.LookupClassification

trait DeviceBus {
  def publish(reading: Reading[Device]): Unit
  def subscribe(subscriber: ActorRef, device: Device): Boolean
  def unsubscribe(subscriber: ActorRef, from: Device): Boolean
  def unsubscribe(subscriber: ActorRef): Unit
}

class AkkaDeviceBus extends DeviceBus
                       with EventBus with LookupClassification {

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
  override protected def mapSize: Int = 128
}
