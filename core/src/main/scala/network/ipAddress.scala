package uk.co.sprily.dh
package harvester
package network

import java.net.InetAddress

sealed trait IPAddress {
  def bytes: Seq[Byte]
  def inet: InetAddress = InetAddress.getByAddress(bytes.toArray)
}

case class IP4Address(raw: (Byte, Byte, Byte, Byte)) extends IPAddress {
  def bytes = List(raw._1, raw._2, raw._3, raw._4) 
}
