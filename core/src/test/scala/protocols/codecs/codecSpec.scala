package uk.co.sprily.dh
package harvester
package protocols
package codecs

import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike
import org.specs2.matcher.Parameters

import scodec.Codec
import scodec.bits.BitVector
import scalaz.\/-

trait CodecSpecUtil extends SpecificationLike with ScalaCheck {

  implicit val ps = Parameters(minTestsOk=300)

  def roundTrip[T](codec: Codec[T], t: T) = {
    val \/-(encoded) = codec.encode(t)
    val \/-((remaining, decoded)) = codec.decode(encoded)
    (remaining must === (empty)) and
    (decoded   must === (t))
  }

  def empty = BitVector.empty

}
