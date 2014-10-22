package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

trait TargetLike {

  def initialDelay(): FiniteDuration = initialDelayFrom(Instant.now())
  def timeoutDelay(): FiniteDuration = timeoutDelayFrom(Instant.now())

  protected[scheduling] val initiateAt: Deadline
  protected[scheduling] val timeoutAt: Deadline

  protected[scheduling] def initialDelayFrom(now: Instant) = {
    initiateAt.timeLeftAt(now) max Duration.Zero
  }

  protected[scheduling] def timeoutDelayFrom(now: Instant) = {
    timeoutAt.timeLeftAt(now) max Duration.Zero
  }
}

