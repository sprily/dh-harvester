package uk.co.sprily.dh
package harvester
package capture

import network.Device
import scheduling.Schedule
import scheduling.TargetLike

case class Request[D <: Device](
    val id: Long,
    val schedule: Schedule,
    val device: D,
    val selection: D#AddressSelection)
