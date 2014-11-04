package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

object RetryTest {
  val s1 = Schedule.each(59.seconds)
                   .retryEvery(9.seconds)
  val s2 = s1.fixTimeoutTo(22.seconds)

  val now = Instant.now()

  val init1 = s1.startAt(now)
  val init2 = s2.startAt(now)

  val f1 = s1.timedOutAt(init1, now+56.seconds)
  val f2 = s2.timedOutAt(init2, now+56.seconds)
}

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
