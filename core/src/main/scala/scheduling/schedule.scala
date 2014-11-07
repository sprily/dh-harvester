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
  def completedAt(previous: Target, now: Instant): Option[Target]
  def timedOutAt(previous: Target, now: Instant): Option[Target]

  /* derived methods */
  def start(): Target = startAt(now())
  def completed(previous: Target): Option[Target] = completedAt(previous, now())
  def timedOut(previous: Target): Option[Target] = timedOutAt(previous, now())

  /* helper methods */
  private def now() = Instant.now()

}

object Schedule {

  def each(interval: FiniteDuration): Schedule = Each(interval)
  def delay(schedule: Schedule, delay: FiniteDuration): Schedule = Delay(schedule, delay)
  def fixedTimeout(s: Schedule, timeout: FiniteDuration): Schedule = FixedTimeout(s, timeout)
  def single(timeout: FiniteDuration) = each(0.seconds).take(1).fixTimeoutTo(timeout)
  def take(s: Schedule, limit: Long): Schedule = Take(s, limit)
  def retry(s: Schedule, retry: FiniteDuration): Schedule = Retry(s, retry)
  def union(s1: Schedule, s2: Schedule): Schedule = Union(s1,s2)

  implicit class ScheduleOps(s: Schedule) {
    def delayBy(delay: FiniteDuration) = Schedule.delay(s, delay)
    def take(n: Long) = Schedule.take(s, n)
    def unionWith(s2: Schedule) = Schedule.union(s, s2)
    def fixTimeoutTo(timeout: FiniteDuration) = Schedule.fixedTimeout(s, timeout)
    def retryEvery(retry: FiniteDuration) = Schedule.retry(s, retry)
  }

}
