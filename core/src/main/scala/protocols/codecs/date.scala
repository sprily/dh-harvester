package uk.co.sprily.dh
package harvester
package protocols
package codecs

import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime

import scodec._
import codecs._
import scalaz.\/


/** encoding/decoding joda date/time structures is achieved by
  * encoding/decoding *through* this `Timestamp` case class
  */
private case class Timestamp(epoch: Long, tzId: String) {
  def toLocalDateTime = new LocalDateTime(epoch, DateTimeZone.forID(tzId))
}

private object Timestamp {

  def apply(ldt: LocalDateTime): Timestamp = {
    val dt = ldt.toDateTime(DateTimeZone.UTC)
    Timestamp(dt.getMillis, DateTimeZone.UTC.getID)
  }

  def codec = (int64 :: variableSizeBits(uint8, utf8)).as[Timestamp]
}
