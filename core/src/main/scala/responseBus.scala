package uk.co.sprily.dh
package harvester

import akka.actor.ActorRef
import akka.event.EventBus
import akka.event.LookupClassification

import capture.ResponseLike

trait ResponseBus {
  def publish(response: ResponseLike): Unit
  def subscribe(subscriber: ActorRef): Boolean
  def unsubscribe(subscriber: ActorRef): Boolean
}

/** Implementation based on Akka's EventBus.
  *
  * Going through Akka's EventBus means we lose track of the
  * types.  It's not such a big issue, as on the way in, we can
  * safely up-cast.  And on the way out, we're dealing with
  * actors anyway (ie AnyRef => Unit).
  */
class AkkaResponseBus extends ResponseBus {

  case object CatchAll

  def publish(response: ResponseLike): Unit = {
    underlying.publish(response)
  }

  def subscribe(subscriber: ActorRef) = {
    underlying.subscribe(subscriber, CatchAll)
  }

  def unsubscribe(subscriber: ActorRef) = {
    underlying.unsubscribe(subscriber, CatchAll)
  }

  val underlying = new EventBus with LookupClassification {
    type Event = ResponseLike
    type Classifier = CatchAll.type
    type Subscriber = ActorRef
     
    // is used for extracting the classifier from the incoming events
    override protected def classify(event: Event): Classifier = CatchAll
     
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
    override protected def mapSize: Int = 1
  }
}
