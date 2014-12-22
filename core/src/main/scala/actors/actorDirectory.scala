package uk.co.sprily.dh
package harvester
package capture

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props

/** Manage a set of (homogenously typed) Actors under a common parent.
  *
  * Given a lookup key, `Key`, an existing `ActorRef` can be obtained, or
  * if it doesn't already exist, it will be created.  The `ActorRef` is then
  * "returned" to the `sender`.
  *
  * See `DeviceActorDirectory` for an example.
  */
abstract class ActorDirectory extends Actor with ActorLogging {

  import ActorDirectory._

  /** How to obtain a child `ActorRef`. **/
  type Key

  /** The message protocol which the `ActorDirectory` implementation uses. **/
  val protocol: Protocol[Key]
  import protocol._

  /** Define a unqiue `String` for a given `Key`.
    *
    * This becomes a new `Actor`'s path, so needs to be unique.
    */
  protected def actorPath(key: Key): String

  private[this] var registeredProps: PropsFactory[Key] = Map.empty

  final def receive = {
    case Lookup(key) =>
      lookup(key) match {
        case Some(child) => sender ! child
        case None        => log.error(s"No Actor found for $key")
      }

    case Forward(key, msg) =>
      lookup(key) foreach (_ forward msg)

    case Register(propsF) =>
      registeredProps = propsF orElse registeredProps

  }


  private final def lookup(key: Key): Option[ActorRef] = {
    val path = actorPath(key)
    if (!context.child(path).isDefined) {
      propsFor(key).map { props =>
        context.actorOf(props, path)
      }
    }
    context.child(path)
  }

  private final def propsFor(key: Key): Option[Props] = {
    registeredProps.lift(key)
  }

}

object ActorDirectory {

  /** A *partial* function used to create a new `Props` instance.
    *
    * Used by the `ActorDirectory` to create a new `Actor` when a child for a
    * given `k: K` is not found.
    *
    * As it's a partial function, it need not necessarily return anything.
    */
  type PropsFactory[K] = PartialFunction[K, Props]

  trait Protocol[K] {

    /** Lookup an `ActorRef` for the given `Key`. **/
    case class Lookup(key: K)

    /** Register a function that creates a new `Props` for a given `k: K` **/
    case class Register(props: PropsFactory[K])

    /** Forward the given message to the `ActorRef` looked-up by the key **/
    case class Forward(key: K, msg: Any)
  }

}
