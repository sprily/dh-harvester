package uk.co.sprily.dh
package harvester
package capture

import network.Device
import scheduling.Schedule
import scheduling.TargetLike

trait RequestLike[D <: Device] {
  val id: Long
  val device: D
  val selection: D#AddressSelection
}

/** Repeating request to capture data from a given Device **/
case class PersistentRequest[D <: Device](
    val id: Long,
    val schedule: Schedule,
    val device: D,
    val selection: D#AddressSelection) extends RequestLike[D]

/** Single request to capture data from a given Device **/
case class AdhocRequest[D <: Device](
    val id: Long,
    val target: TargetLike,
    val device: D,
    val selection: D#AddressSelection) extends RequestLike[D]
