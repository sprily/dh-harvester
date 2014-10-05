package uk.co.sprily.dh
package harvester

import scala.concurrent.duration._

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

  def diff(first: LocalDateTime, second: LocalDateTime) = {
    new JDuration(
      first.toDateTime,
      second.toDateTime
    ).getMillis.millis
  }
}

trait ScheduleInfo {
  def delay: FiniteDuration
  def timeout: FiniteDuration
}

/**
  * Targets a uniformly distributed sequence of Schedules.
  *
  * Eg. given an interval of 5 seconds, this Target will create Schedules for
  * 0s, 5s, 10s, 15s ...
  *
  * The issued Schedule times out if the next target-point is missed.  eg. in
  * the above example, the Schedule that targets the 10s mark will include a
  * timeout for if the 15s mark is missed.
  */
case class Each(interval: FiniteDuration) extends Target {

  import Target._

  case class Schedule(
      val delay: FiniteDuration,
      val issuedAt: LocalDateTime,
      val targetTimePoint: LocalDateTime) extends ScheduleInfo {

    val timeout = diff(issuedAt, targetTimePoint) min interval
  }

  /** start immediately **/
  def start(now: LocalDateTime) = Schedule(
    delay = Duration.Zero,
    issuedAt = now,
    targetTimePoint = now + interval)

  def completed(previous: Schedule, now: LocalDateTime) = {

    diff(now, previous.targetTimePoint) match {
      case delta if delta >= Duration.Zero => Schedule(
        delay = delta,
        issuedAt = now,
        targetTimePoint = previous.targetTimePoint + interval)
      case delta if delta < Duration.Zero => Schedule(
        delay = Duration.Zero,
        issuedAt = now,
        targetTimePoint = previous.targetTimePoint + Math.round(-delta/interval + 0.5) * interval)
    }
  }

  def timedOut(previous: Schedule, now: LocalDateTime) = ???

}

object Target {

  implicit class JodaLocalDateTimeOps(ts: LocalDateTime) {
    def +(d: FiniteDuration): LocalDateTime = {
      ts.plus(JDuration.millis(d.toMillis))
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
