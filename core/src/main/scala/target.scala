package uk.co.sprily
package dh
package harvester

import scala.concurrent.duration._

sealed trait Target {
  def timeout: FiniteDuration
}

case class Once(timeout: FiniteDuration) extends Target
case class Every(interval: FiniteDuration, grace: FiniteDuration) extends Target {
  def timeout = interval + grace
}
case class Within(
    window: (FiniteDuration, FiniteDuration),
    grace: FiniteDuration) extends Target {

  def timeout = upper + grace
  def lower = window._1
  def upper = window._2
  def midpoint = (lower+upper) / 2
}
