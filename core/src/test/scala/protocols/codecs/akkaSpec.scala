package uk.co.sprily.dh
package harvester
package protocols
package codecs

import akka.util.ByteString

import org.scalacheck.Arbitrary
import org.scalacheck.Gen

class AkkaSpec extends CodecSpecUtil {

  "Akka ByteString Codec" should {
    "complete successful round trip" in {
      prop {
        (bs: ByteString) => roundTrip(byteString, bs)
      }
    }
  }

  implicit def arbitraryByteString: Arbitrary[ByteString] = Arbitrary(
    Gen.listOf(Gen.choose(0,255).map(_.toByte)).map(bs => ByteString(bs.toArray))
  )

}

