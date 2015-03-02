package uk.co.sprily.dh
package harvester
package network

import java.net.UnknownHostException
import java.net.InetAddress

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import scalaz._
import Scalaz._

sealed trait IPAddress {
  def bytes: Seq[Byte]
  def inet: InetAddress = InetAddress.getByAddress(bytes.toArray)
}

case class IP4Address(raw: (Byte, Byte, Byte, Byte)) extends IPAddress {
  def bytes = List(raw._1, raw._2, raw._3, raw._4) 
  override def toString = bytes.mkString(".")
}

object IP4Address {

  lazy val localhost = IP4Address((127,0,0,1))

  def apply(raw1: Byte, raw2: Byte, raw3: Byte, raw4: Byte): IP4Address = {
    IP4Address((raw1, raw2, raw3, raw4))
  }

  @deprecated("Use validated() instead", since="time began")
  def fromString(host: String): Option[IP4Address] = host match {
    case "localhost" => Some(localhost)
    case _           => None
  }

  def validated(host: String): ValidationNel[String, IP4Address] = host.toLowerCase match {
    case "localhost" => localhost.success
    case "127.0.0.1" => localhost.success
    case other       => try {
        val bytes = InetAddress.getByName(other).getAddress
        IP4Address(bytes(0), bytes(1), bytes(2), bytes(3)).success
      } catch {
        case (e: UnknownHostException) => "Unknown host".failureNel
        case (e: SecurityException)    => "JVM checkConnect() not allowed.".failureNel
      }
    }

}
