package uk.co.sprily.dh
package harvester

import scala.concurrent.duration._

import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.joda.time.{Duration => JDuration}

trait Target {

  type Schedule <: ScheduleInfo

  def start(now: LocalDateTime): Schedule
  def completed(previous: Schedule, now: LocalDateTime): Schedule
  def timedOut(previous: Schedule, now: LocalDateTime): Schedule

  /* derived methods */
  def start(): Schedule = start(now())
  def completed(previous: Schedule): Schedule = completed(previous, now())
  def timedOut(previous: Schedule): Schedule = timedOut(previous, now())

  /* helper methods */
  def now() = LocalDateTime.now()

}

trait ScheduleInfo {
  def delay: FiniteDuration
  def timeout: FiniteDuration
}

/**
  * Targets a regularly distributed sequence of Schedules.
  *
  * Eg. given an interval of 5 seconds, this Target will create Schedules for
  * 0s, 5s, 10s, 15s ...
  *
  * The issued Schedule times out if the next deadline is missed.  eg. in
  * the above example, the Schedule that targets the 10s mark will include a
  * timeout for if the 15s mark is missed.  Note, however, that it is still
  * possible to complete a Schedule, even if it didn't timeout:
  *
  * Completing a Schedule after the targeted deadline re-issues a new Schedule
  * targeting the next valid deadline, keeping the deadlines on the original
  * latice.  eg - if the 5s deadline was completed at 6s, then the issued
  * Schedule will not be delayed at all, and the timeout set to 4 seconds (10s
  * - 6s), rather the 5s.
  *
  * Furthermore, if any interim deadlines are missed, then
  * they are ignoruad.  eg - if the 5s deadline was completed at 12s, then the
  * issued Schedule will not be delayed, and the timeout will be set to 3
  * seconds (15s - 12s)
  */
case class Each(interval: FiniteDuration) extends Target {

  import Target._

  case class Schedule(
      val delay: FiniteDuration,
      val timeout: FiniteDuration,
      val deadline: LocalDateTime) extends ScheduleInfo

  /** start immediately **/
  def start(now: LocalDateTime) = Schedule(
    delay = Duration.Zero,
    timeout = interval,
    deadline = now + interval)

  def completed(previous: Schedule, now: LocalDateTime) = {

    val delta = previous.deadline - now
    val deadline = delta match {
      case delta if delta >= Duration.Zero =>
        previous.deadline + interval
      case delta if delta < Duration.Zero =>
        previous.deadline + Math.round(-delta/interval + 0.5) * interval
    }

    Schedule(
      delay = delta max Duration.Zero,
      timeout = (deadline - now) min interval,
      deadline = deadline
    )
  }

  def timedOut(previous: Schedule, now: LocalDateTime) = ???

}

object Target {

  implicit class JodaLocalDateTimeOps(ts: LocalDateTime) {

    def +(d: FiniteDuration): LocalDateTime = {
      ts.plus(JDuration.millis(d.toMillis))
    }

    def -(d: FiniteDuration): LocalDateTime = {
      ts + (-d)
    }

    def -(ts2: LocalDateTime) = {
      new JDuration(
        ts2.toDateTime(DateTimeZone.UTC),
        ts.toDateTime(DateTimeZone.UTC)
      ).getMillis.millis
    }
  }

}

//  private[this] def timeNow() = LocalDateTime.now()
//
//  def nextTrigger(target: Target,
//                  lastTriggered: LocalDateTime,
//                  now: LocalDateTime = LocalDateTime.now())
//                 : TriggerAt = {
//
//    val diff = new JDuration(
//      lastTriggered.toDateTime,
//      now.toDateTime
//    ).getMillis.millis
//
//    val waitFor = target match {
//      case Every(interval, _)  if (diff >= interval) => Duration.Zero
//      case Every(interval, _)                        => interval - diff
//      case Within(_, upper, _) if (diff >= upper)    => Duration.Zero
//      case Within(lower, _, _) if (diff >= lower)    => Duration.Zero
//      case t@Within(_, _, _)                         => t.midpoint - diff
//    }
//
//    Trigger(waitFor, target.timeout)
//  }
//}
//
///**
//  * Target the trigger to occur every `interval` period
//  */
//case class Every(interval: FiniteDuration, grace: FiniteDuration) extends Target {
//  def timeout = interval + grace
//}
//
///**
//  * Target the trigger to occur between the given bounds
//  */
//case class Within(lower: FiniteDuration, upper: FiniteDuration, grace: FiniteDuration) extends Target {
//  def timeout = upper + grace
//  def midpoint = (lower+upper) / 2
//}
