package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

case class FixedTimeout(schedule: Schedule, timeout: FiniteDuration) extends Schedule {

  assert(timeout > Duration.Zero)

  case class Target(
      val initiateAt: Deadline,
      val timeoutAt: Deadline,
      val underlying: schedule.Target) extends TargetLike

  object Target {
    def apply(underlying: schedule.Target): Target = Target(
      initiateAt = underlying.initiateAt,
      timeoutAt = underlying.initiateAt + timeout,
      underlying = underlying)
  }

  override def startAt(now: Instant) = {
    val underlying = schedule.startAt(now)
    Target(underlying)
  }

  override def completedAt(previous: Target, now: Instant) = {
    schedule.completedAt(previous.underlying, now)
            .map(Target.apply _)
  }

  override def timedOutAt(previous: Target, now: Instant) = {
    schedule.timedOutAt(previous.underlying, now)
            .map(Target.apply _)
  }

}
