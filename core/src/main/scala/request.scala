package uk.co.sprily
package dh
package harvester

case class PersistentRequest[D <: Device](
    val id: Long,
    val target: Target,
    val device: D,
    val range: D#RegisterSelection)
