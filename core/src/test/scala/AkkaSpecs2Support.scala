package uk.co.sprily.dh
package harvester

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import akka.testkit.TestKit

import org.specs2.mutable.After

/**
  * NOTE - if sub-classing this, don't put anything in the body
  * of the constructor of the sub-class.  `After` subclasses
  * `DelayedInit`, and the way `DelayedInit` works is to call
  * `delayedInit()` after *each* constructor in the class-hierarchy.
  * The outcome of this is that the actor system will be shutdown
  * before the test even starts.
  *
  * methods and lazy vals are OK in the body, just not vals or vars.
  */
class AkkaSpecs2Support extends TestKit(ActorSystem("test-system"))
                           with After {
  override def after = {
    system.shutdown()
  }

  /** An Actor that forwards all messages on to a next Actor **/
  class ForwardingActor(
      next: ActorRef = testActor,
      respondWith: PartialFunction[Any,Option[Any]] = noResponse) extends Actor {

    def receive = {
      case msg =>
        next forward msg
        respondWith(msg) foreach { sender ! _ }
    }
  }

  class EchoActor() extends Actor {
    def receive = {
      case msg => sender ! msg
    }
  }

  class DiscardingActor() extends Actor {
    def receive = {
      case msg => {}
    }
  }

  def logExceptions(props: Props, logTo:ActorRef = testActor) = {
    system.actorOf(Props(new Actor {
      val child = context.actorOf(props)

      override val supervisorStrategy = OneForOneStrategy() {
        case f => logTo ! f ; Stop
      }

      def receive = { case m => child forward m }

    }))
  }

  private final def noResponse: PartialFunction[Any, Option[Any]] = { case _ => None }

}
