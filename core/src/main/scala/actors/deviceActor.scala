package uk.co.sprily.dh
package harvester
package capture

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props

import network.Device

class DeviceActor extends Actor with ActorLogging {

  import DeviceActor.Protocol._

  private[this] var children = Map[Device, ActorRef]()
  private[this] var propSelections: PropsSelection = Map.empty

  def receive = {
    case msg@SampleDevice(request) =>
      lookup(request.device) match {
        case Some(child) => child forward msg
        case None        =>
          log.error(s"No DeviceActor found for ${request.device}")
      }

    case msg@RegisterDevice(props) =>
      propSelections = props orElse propSelections
  }

  private def lookup(device: Device): Option[ActorRef] = {
    if (! children.contains(device)) {
      propsFor(device).map { props =>
        context.actorOf(props, device.id.v.toString)
      }.foreach { child =>
        children = children + (device -> child)
      }
    }
    children.get(device)
  }

  private def propsFor(device: Device): Option[Props] = {
    propSelections.lift(device)
  }

}

object DeviceActor {
  object Protocol {

    type PropsSelection = PartialFunction[Device,Props]

    case class SampleDevice(request: Request2)
    case class RegisterDevice(props: PropsSelection)
  }
}
