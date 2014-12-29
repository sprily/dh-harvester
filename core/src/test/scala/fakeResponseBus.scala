package uk.co.sprily.dh
package harvester

import akka.actor.ActorRef

import capture.Response

class FakeResponseBus extends ResponseBus {
  var responses = List[Response]()
  def publish(r: Response) = { responses = r :: responses }
  def subscribe(subscriber: ActorRef): Boolean = ???
  def unsubscribe(subscriber: ActorRef): Boolean = ???
}
