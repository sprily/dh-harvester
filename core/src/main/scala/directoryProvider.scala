package uk.co.sprily.dh
package harvester

import scala.reflect.runtime.universe._

import akka.actor.ActorSystem

import modbus.ModbusDevice
import modbus.ModbusActorDirectory

import network.Device

trait DirectoryProvider {

  protected val modbus: DeviceActorDirectory[ModbusDevice]

  def lookup[D <: Device : TypeTag]
            :Option[DeviceActorDirectory[D]] = typeOf[D] match {

    case tt if tt =:= typeOfModbusDevice =>
      Some(modbus.asInstanceOf[DeviceActorDirectory[D]])

    case unknown => None
  }

  // Memoized for efficiency
  private lazy val typeOfModbusDevice = typeOf[ModbusDevice]
}

class DefaultDirectoryProvider(system: ActorSystem)
    extends DirectoryProvider {

  override val modbus = new ModbusActorDirectory(system)

}

