package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

case class Take(schedule: Schedule, n: Long) extends Schedule {

  case class Target(
      val initiateAt: Deadline,
      val timeoutAt: Deadline,
      val numLeft: Long,
      val underlying: schedule.Target) extends TargetLike

  object Target {
    def apply(numLeft: Long)(underlying: schedule.Target): Target = Target(
      initiateAt = underlying.initiateAt,
      timeoutAt = underlying.timeoutAt,
      numLeft = numLeft,
      underlying = underlying)
  }

  override def startAt(now: Instant) = {
    val underlying = schedule.startAt(now)
    Target(n-1)(underlying)
  }

  override def completedAt(previous: Target, now: Instant) = {
    previous.numLeft match {
      case numLeft if numLeft > 0 =>
        schedule.completedAt(previous.underlying, now)
                .map(Target.apply(numLeft-1) _)
      case _                      => None
    }
  }

  override def timedOutAt(previous: Target, now: Instant) = {
    previous.numLeft match {
      case numLeft if numLeft > 0 =>
        schedule.timedOutAt(previous.underlying, now)
                .map(Target.apply(numLeft-1) _)
      case _                      => None
    }
  }

}
