package uk.co.sprily.dh
package harvester

import network.Device
import scheduling.Schedule

case class PersistentRequest[D <: Device](
    val id: Long,
    val schedule: Schedule,
    val device: D,
    val selection: D#AddressSelection)
