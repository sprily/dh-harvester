package uk.co.sprily.dh
package harvester

import java.nio.charset.Charset

import org.joda.time.LocalDateTime

trait Serialiser[T] {
  def toBytes(t: T): Seq[Byte]
}

object Serialiser {

  val `UTF-8` = Charset.forName("UTF-8")

  implicit def localDateTime: Serialiser[LocalDateTime] = new Serialiser[LocalDateTime] {
    override def toBytes(dt: LocalDateTime) = {
      dt.toString().getBytes(`UTF-8`)
    }
  }
}
