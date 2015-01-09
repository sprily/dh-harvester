package uk.co.sprily.dh
package harvester

import akka.actor.ActorRef

import capture.ResponseLike

class FakeResponseBus extends ResponseBus {
  var responses = List[ResponseLike]()
  def publish(r: ResponseLike) = { responses = r :: responses }
  def subscribe(subscriber: ActorRef): Boolean = ???
  def unsubscribe(subscriber: ActorRef): Boolean = ???
}
