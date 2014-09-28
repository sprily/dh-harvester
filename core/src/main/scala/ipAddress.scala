package uk.co.sprily
package dh
package harvester

sealed trait IPAddress
case class IP4Address(raw: (Byte, Byte, Byte, Byte)) extends IPAddress
case class IP6Address(raw: (Byte, Byte, Byte, Byte)) extends IPAddress