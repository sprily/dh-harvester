package uk.co.sprily.dh
package harvester

import network.Device
import scheduling.Schedule
import scheduling.TargetLike

case class PersistentRequest[D <: Device](
    val id: Long,
    val schedule: Schedule,
    val device: D,
    val selection: D#AddressSelection)

case class AdhocRequest[D <: Device](
    val id: Long,
    val target: TargetLike,
    val device: D,
    val selection: D#AddressSelection)
