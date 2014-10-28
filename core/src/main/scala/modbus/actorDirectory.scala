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

class ModbusActorDirectory(system: ActorSystem)
    extends DeviceActorDirectoryImpl[ModbusDevice](system) {

  type NetLoc = TCPGateway

  def netLocFor(d: ModbusDevice) = d.address.gateway
  def workerProps(netLoc: NetLoc) = ConnectionActor.gateway(netLoc, this, 1)
}
