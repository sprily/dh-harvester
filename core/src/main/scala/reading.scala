package uk.co.sprily.dh
package harvester

import org.joda.time.LocalDateTime

import network.Device

case class Reading[+D <: Device](
    val timestamp: LocalDateTime,
    val device: D,
    val measurement: D#Measurement)
