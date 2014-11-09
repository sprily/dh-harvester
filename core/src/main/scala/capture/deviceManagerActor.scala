package uk.co.sprily.dh
package harvester
package capture

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.OneForOneStrategy
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.actor.SupervisorStrategy._
import akka.actor.Terminated

import modbus.ModbusDevice
import network.Device

class DeviceManagerActor(
    provider: DirectoryProvider,
    implicit val bus: DeviceBus) extends Actor with ActorLogging {

  import DeviceManagerActor.Child
  import DeviceManagerActor.Protocol._
  import RequestActor.PollingTimedOutException

  // Actor state
  var requests = Map[Long, Child]()

  // Akka stuff
  override val supervisorStrategy = OneForOneStrategy() {
    case _: PollingTimedOutException => Resume
  }

  def receive = {
    case (r: ModbusRequest)       => ensureRequest(r)
    case (rs: PersistentRequests) => ensurePersistentRequests(rs)
    case Terminated(a)            => handleChildTerminated(a)
  }

  def ensurePersistentRequests(rs: PersistentRequests) = {
    log.info(s"Ensuring PersistentRequests are set to run")
    val activeIds = rs.requests.map(_.r.id).toSet
    requests.foreach { case (id, child) =>
      if (! activeIds.contains(id)) stopRequest(child.dr)
    }
    rs.requests foreach ensureRequest
  }

  /** Ensure that a Child is set up to handle the given DeviceRequest.
    *
    * All the boiler plate is to re-establish the type information that's
    * lost in the DeviceRequest
    */
  def ensureRequest(dr: DeviceRequest) = {
    log.info(s"Ensuring DeviceRequest $dr")

    dr match {
      case ModbusRequest(r) =>
        implicit val req = r
        implicit val d = provider.lookup[ModbusDevice].get
        requests.get(r.id) match {
          case None                          => spawnChild(dr)
          case Some(child) if child.dr != dr => replaceChild(dr)
          case _                             => ()
        }
    }

  }

  def stopRequest(dr: DeviceRequest) = {
    log.info(s"Stopping request $dr.r.id")
    requests.get(dr.r.id).foreach { child =>
      child.ref ! PoisonPill.getInstance
      requests = requests - dr.r.id
    }
  }

  def handleChildTerminated(a: ActorRef) = {
    log.info(s"Child RequestActor terminated $a")
    requests = requests.filter { case (_, Child(_, ref)) => a != ref  }
  }

  private def spawnChild[D <: Device](dr: DeviceRequest)
                                     (implicit d: DeviceActorDirectory[D],
                                               req: Request[D]) = {
    val ref = context.actorOf(RequestActor.props(req))
    requests = requests + (req.id -> Child(dr, ref))
    context.watch(ref)
  }

  private def replaceChild[D <: Device](dr: DeviceRequest)
                                       (implicit d: DeviceActorDirectory[D],
                                                 req: Request[D]) = {
    requests(dr.r.id).ref ! PoisonPill.getInstance
    spawnChild(dr)
  }
}

object DeviceManagerActor {

  protected case class Child(dr: Protocol.DeviceRequest, ref: ActorRef)

  object Protocol {

    /** Due to type erasure, we define concrete messages to encode Requests
      * for each type  of Device.
      */
    sealed trait DeviceRequest { def r: Request[_ <: Device] }
    case class ModbusRequest(r: Request[ModbusDevice]) extends DeviceRequest

    /** A collection of DeviceRequests which together form the complete set
      * of persistent (non-adhoc) requests that should be managed.
      *
      * Sending a PersistentRequest message to the device manager actor will
      * result in the replacement of the existing requests.
      *
      * TODO: this will also replace existing adhoc requests as things
      *       currently stand.  Re-visit this when thinking about the request
      *       IDs.
      */
    case class PersistentRequests(requests: Seq[DeviceRequest])
  }
}
