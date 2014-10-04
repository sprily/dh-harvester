package uk.co.sprily.dh
package harvester
package network

case class TCPGateway(
    val address: IPAddress,
    val port: Int)
