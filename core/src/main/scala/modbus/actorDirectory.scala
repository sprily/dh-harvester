package uk.co.sprily.dh
package harvester
package modbus

import akka.actor.ActorContext
import akka.actor.ActorSelection

trait ActorDirectory extends DeviceActorDirectoryService[ModbusDevice] {

  //def lookup(d: ModbusDevice)
  //          (implicit context: ActorContext): ActorSelection = {
  //  ???
  //}

}
