package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

case class Delay(schedule: Schedule, delay: FiniteDuration) extends Schedule {
    
  case class Target(
      val initiateAt: Deadline,
      val timeoutAt: Deadline,
      val underlying: schedule.Target) extends TargetLike

  override def startAt(now: Instant) = {
    val underlying = schedule.startAt(now + delay)
    Target(
      initiateAt = underlying.initiateAt,
      timeoutAt = underlying.timeoutAt,
      underlying = underlying)
  }

  override def completedAt(previous: Target, now: Instant) = {
    val underlying = schedule.completedAt(
      previous.underlying,
      now)
    Target(
      initiateAt = underlying.initiateAt,
      timeoutAt = underlying.timeoutAt,
      underlying = underlying)
  }
}
