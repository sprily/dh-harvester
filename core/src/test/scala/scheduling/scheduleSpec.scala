package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.matcher.Parameters
import org.specs2.time.NoTimeConversions

object ScheduleSpec extends Specification with ScalaCheck
                                          with NoTimeConversions {

  def meaningfulTargetProperty(implicit schedules: Arbitrary[Schedule],
                                        completionTimes: Arbitrary[FiniteDuration]) = {
    prop {
      (s: Schedule, completions: Seq[(FiniteDuration, Boolean)]) => {

        val baseTime = Instant.now()
        val trace = traceExecutionInfo(baseTime)(completions)(s)(chooseStep(s))

        trace must contain { m: MomentInfo =>
          (m.measuredDelay must beGreaterThanOrEqualTo(Duration.Zero)) and
          (m.measuredTimeout must beGreaterThan(m.measuredDelay))
        }.foreach

      }
    }
  }

  case class Moment[T <: Schedule#Target](time: Instant, target: T)

  case class MomentInfo(time: Instant, target: TargetInfo) {
    val measuredDelay = target.initialDelayFrom(time)
    val measuredTimeout = target.timeoutDelayFrom(time)
  }

  case class TargetInfo(
      val initiateAt: Deadline,
      val timeoutAt: Deadline) extends TargetLike

  def traceExecution[S <: Schedule]
                    (baseTime: Instant)
                    (completions: Seq[(FiniteDuration, Boolean)])
                    (s: S)
                    (step: Boolean => (s.Target, Instant) => s.Target): Seq[Moment[s.Target]] = {

    val init = Moment(baseTime, s.startAt(baseTime))

    val accumulate = { (prev: Moment[s.Target], next: (FiniteDuration, Boolean)) =>
      val (delta, completed) = next
      val now = prev.time + delta
      Moment(now, step(completed)(prev.target, now))
    }

    completions.scanLeft(init)(accumulate)
  }

  def traceExecutionInfo(baseTime: Instant)
                        (completions: Seq[(FiniteDuration, Boolean)])
                        (s: Schedule)
                        (step: Boolean => (s.Target, Instant) => s.Target): Seq[MomentInfo] = {
    traceExecution(baseTime)(completions)(s)(step).map { moment =>
      MomentInfo(moment.time, TargetInfo(moment.target.initiateAt,
                                         moment.target.timeoutAt))
    }
  }

  def chooseStep[S <: Schedule](s: S)
                               (completed: Boolean)
                               (prev: s.Target, now: Instant): s.Target = {
    if (completed) s.completedAt(prev, now) else s.timedOutAt(prev, now)
  }

}


