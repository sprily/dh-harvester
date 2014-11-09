package uk.co.sprily.dh
package harvester

trait Serialiser[T] {
  def toBytes(t: T): Seq[Byte]
}
