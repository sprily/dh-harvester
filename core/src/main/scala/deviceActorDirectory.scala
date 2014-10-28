package uk.co.sprily.dh
package harvester

import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorLogging
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props

import org.joda.time.LocalDateTime

import network.Device

trait DeviceActorDirectory[D <: Device] {

  object Protocol {
    case class Poll(d: D, selection: D#AddressSelection)
    case class Result(timestamp: LocalDateTime, measurement: D#Measurement)
  }

  def lookup(device: D): ActorSelection
}

abstract class DeviceActorDirectoryImpl[D <: Device]
                                       (system: ActorSystem)
                                          extends DeviceActorDirectory[D] {

  import Protocol.Poll
  private val router = system.actorOf(Props(new Router()))
  override def lookup(device: D) = system.actorSelection(router.path)

  /** Abstract members to define how to group Devices by network location. */
  protected type NetLoc
  protected def netLocFor(device: D): NetLoc

  /** How to create a new actor responsible for communicating with
    * the given network location.
    */
  protected def workerProps(netLoc: NetLoc): Props

  /** An Actor which forwards Poll messages to the actor capable of
    * fulfilling the request.
    *
    * Creates new child actors as necessary.
    */
  private class Router extends Actor with ActorLogging {

    private var locs = Map[NetLoc, ActorRef]()

    def receive = {
      case p@Poll(device, _) => lookupActor(device) forward p
    }

    def lookupActor(d: D): ActorRef = {
      val netLoc = netLocFor(d)
      if(! locs.contains(netLoc)) {
        val a = context.actorOf(workerProps(netLoc))
        locs += netLoc -> a
      }

      locs.get(netLoc).get
    }
  }
}
