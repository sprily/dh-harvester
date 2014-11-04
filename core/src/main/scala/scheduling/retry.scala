package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

case class Retry(schedule: Schedule, retry: FiniteDuration) extends Schedule {

  case class Target(
      val initiateAt: Deadline,
      val timeoutAt: Deadline,
      val underlying: schedule.Target) extends TargetLike

  override def startAt(now: Instant) = {
    val underlying = schedule.startAt(now)

    Target(
      initiateAt = underlying.initiateAt,
      timeoutAt = underlying.timeoutAt,
      underlying = underlying)
  }

  override def completedAt(previous: Target, now: Instant) = {
    val underlying = schedule.completedAt(previous.underlying, now)
    Target(
      initiateAt = underlying.initiateAt,
      timeoutAt = underlying.timeoutAt,
      underlying = underlying)
  }

  override def timedOutAt(previous: Target, now: Instant) = {

    val initiateAt = previous.initiateAt + retry
    val timeout = previous.timeoutAt - previous.initiateAt

    Target(
      initiateAt = if (initiateAt >= now) initiateAt else now,
      timeoutAt = if (initiateAt >= now) initiateAt + timeout else now + timeout,
      underlying = previous.underlying)
  }

}
