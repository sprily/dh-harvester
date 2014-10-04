package uk.co.sprily.dh
package harvester

import network.Device

case class PersistentRequest[D <: Device](
    val id: Long,
    val target: Target,
    val device: D,
    val selection: D#AddressSelection)
