package uk.co.sprily.dh
package harvester
package modbus

import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorLogging
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props

import network.TCPGateway

trait ActorDirectory extends DeviceActorDirectory[ModbusDevice] {

  //def lookup(d: ModbusDevice)
  //          (implicit context: ActorContext): ActorSelection = {
  //  ???
  //}

}

class ModbusActorDirectory(system: ActorSystem) extends DeviceActorDirectory[ModbusDevice] { dir =>

  import Protocol.Poll

  private val mgrName = "modbus-gw"
  private val mgrPath = "/user/" + mgrName
  private val mgr = system.actorOf(Props(new Manager()), mgrName)

  def lookup(device: ModbusDevice) = {
    system.actorSelection(mgrPath)
  }

  class Manager extends Actor with ActorLogging {

    private var gws = Map[TCPGateway, ActorRef]()

    def receive = {
      case p@Poll(device, selection) => lookupGatewayFor(device) forward p
    }

    private def lookupGatewayFor(d: ModbusDevice): ActorRef = {
      if (!gws.contains(d.address.gateway)) {

        log.info(s"Creating new modbus GW router for ${d.address.gateway}")

        gws = gws + (d.address.gateway -> context.actorOf(
          ConnectionActor.gateway(d.address.gateway, dir, 1),
          pathNameFor(d.address.gateway)))
          //s"${d.address.gateway.address}:${d.address.gateway.port}"))
      }

      log.debug(s"Looked up GW for ${d}")
      gws.get(d.address.gateway).get
    }

    private def pathNameFor(gw: TCPGateway) = {
      gw.address.bytes.map(_.toString).mkString(".") + ":" + gw.port
    }
  }

}
