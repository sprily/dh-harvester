package uk.co.sprily.dh
package harvester
package scheduling

import org.specs2.time.TimeConversions

/** Hides specs2.Specification's implicit Int conversion.
  * because we want the scala.concurrent.duration ones
  */
trait HideDurationImplicits extends TimeConversions {
  override def intToRichLong(v: Int) = super.intToRichLong(v)
  override def longAsTime(v: Long) = super.longAsTime(v)
}
