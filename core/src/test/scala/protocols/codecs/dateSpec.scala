package uk.co.sprily.dh
package harvester
package protocols
package codecs

import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime

import org.scalacheck.Arbitrary
import org.scalacheck.Gen

class DateSpec extends CodecSpecUtil {

  "Joda LocalDateTime Codec" should {
    "complete successful round trip" in {
      prop {
        (ldt: LocalDateTime) => roundTrip(localDateTime, ldt)
      }
    }
  }

  implicit def arbitraryLocalDateTime: Arbitrary[LocalDateTime] = Arbitrary(for {
    minute <- Gen.choose[Int](0,59)
    hour <- Gen.choose[Int](0,23)
    day <- Gen.choose[Int](1,28)
    month <- Gen.choose[Int](1,12)
    year <- Gen.choose[Int](2000,2100)
  } yield new LocalDateTime(year,month,day,hour,minute))

}

