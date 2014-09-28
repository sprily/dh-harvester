package uk.co.sprily
package dh
package harvester

trait Gateway

case class TCPGateway(
    val address: IPAddress,
    val port: Int)
