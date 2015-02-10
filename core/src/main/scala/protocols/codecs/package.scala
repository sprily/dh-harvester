package uk.co.sprily.dh
package harvester
package protocols

import akka.util.ByteString

import org.joda.time.LocalDateTime

import scodec.Codec

package object codecs {

  /** Codec for Akka's ByteString rope-like data structure **/
  def byteString: Codec[ByteString] = new ByteStringCodec()

  /** A timestamp without timezone info **/
  def localDateTime: Codec[LocalDateTime] = {
    Timestamp.codec.xmap( _.toLocalDateTime, Timestamp(_))
  }
}
