package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

/**
  * Not designed for high-precision scheduling of tasks.
  */
trait Schedule {

  type Target <: TargetLike

  def startAt(now: Instant): Target
  def completedAt(previous: Target, now: Instant): Target
  def timedOutAt(previous: Target, now: Instant): Target

  /* derived methods */
  def start(): Target = startAt(now())
  def completed(previous: Target): Target = completedAt(previous, now())
  def timedOut(previous: Target): Target = timedOutAt(previous, now())

  /* helper methods */
  private def now() = Instant.now()

}

object Schedule {

  def each(interval: FiniteDuration): Schedule = Each(interval)
  def delay(schedule: Schedule, delay: FiniteDuration): Schedule = Delay(schedule, delay)
  def union(s1: Schedule, s2: Schedule): Schedule = Union(s1,s2)

  implicit class ScheduleOps(s: Schedule) {
    def delayBy(delay: FiniteDuration) = Schedule.delay(s, delay)
    def unionWith(s2: Schedule) = Schedule.union(s, s2)
  }

}
