package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

/**
  * Schedules a regularly distributed sequence of Targets.
  *
  * Eg. given an interval of 5 seconds, this Target will create Targets for
  * 0s, 5s, 10s, 15s ...
  *
  * The issued Target times out if the next deadline is missed.  eg. in
  * the above example, the Target that targets the 10s mark will include a
  * timeout for if the 15s mark is missed.  Note, however, that it is still
  * possible to complete a Target, even if it didn't timeout:
  *
  * Completing a Target after the targeted deadline re-issues a new Target
  * targeting the next valid deadline, keeping the deadlines on the original
  * latice.  eg - if the 5s deadline was completed at 6s, then the issued
  * Target will not be delayed at all, and the timeout set to 4 seconds (10s
  * - 6s), rather the 5s.
  *
  * Furthermore, if any interim deadlines are missed, then they are ignored.
  * eg - if the 5s deadline was completed at 12s, then the issued Target will
  * not be delayed, and the timeout will be set to 3 seconds (15s - 12s)
  */
case class Each(interval: FiniteDuration) extends Schedule {

  case class Target(
      val initiateAt: Deadline,
      val timeoutAt: Deadline) extends TargetLike

  /** start immediately **/
  override def startAt(now: Instant) = Target(
    initiateAt = now,
    timeoutAt = now + interval)

  override def completedAt(previous: Target, now: Instant) = {

    val timeLeft = previous.timeoutAt - now
    val timeoutAt: Deadline = timeLeft match {
      case delta if delta >= Duration.Zero =>
        previous.timeoutAt + interval
      case delta if delta < Duration.Zero =>
        previous.timeoutAt + Math.round(-delta/interval + 0.5) * interval
    }

    Target(
      initiateAt = now + (timeLeft.max(Duration.Zero)),
      timeoutAt = timeoutAt
    )
  }

  override def timedOutAt(previous: Target, now: Instant) = completedAt(previous, now)
}

