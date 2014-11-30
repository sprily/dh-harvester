package uk.co.sprily.dh
package harvester
package protocols
package codecs

import akka.util.ByteString

import scodec.Codec
import scodec.bits.BitVector
import scalaz.\/

private[codecs] final class ByteStringCodec extends Codec[ByteString] {

  /** `bs.toArray` provides a copy, so `BitVector.view` is safe to use. **/
  override def encode(bs: ByteString) = {
    \/.right(BitVector.view(bs.toArray))
  }

  override def decode(buffer: BitVector) = {
    \/.right((BitVector.empty, ByteString.fromArray(buffer.toByteArray)))
  }

}
