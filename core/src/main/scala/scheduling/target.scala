package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

trait TargetLike {
  val initiateAt: Deadline
  val timeoutAt: Deadline

  def initialDelay(): FiniteDuration = initialDelayFrom(Instant.now())
  def timeoutDelay(): FiniteDuration = timeoutDelayFrom(Instant.now())

  def initialDelayFrom(now: Instant) = {
    initiateAt.timeLeftAt(now) max Duration.Zero
  }

  def timeoutDelayFrom(now: Instant) = {
    timeoutAt.timeLeftAt(now) max Duration.Zero
  }
}

