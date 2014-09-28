package uk.co.sprily
package dh
package harvester

import scala.concurrent.duration._

sealed trait Target
case object Once extends Target
case class OnceAfter(delay: Duration) extends Target
case class Every(interval: Duration) extends Target
