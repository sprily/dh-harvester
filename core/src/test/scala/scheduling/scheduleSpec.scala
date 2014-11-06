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
        val trace = traceExecution(baseTime)(completions)(s)

        trace must contain { m: MomentInfo =>
          (m.measuredDelay must beGreaterThanOrEqualTo(Duration.Zero)) and
          (m.measuredTimeout must beGreaterThan(m.measuredDelay))
        }.foreach

      }
    }
  }

  def equalTraces(s1: Schedule, s2: Schedule)
                 (implicit completions: Seq[(FiniteDuration, Boolean)]) = {

    val now = Instant.now()

    val trace1 = traceExecution(now)(completions)(s1)
    val trace2 = traceExecution(now)(completions)(s1)

    trace1 must === (trace2)
  }

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
                    (s: S): Seq[MomentInfo] = {

    type Moment = (Instant, s.Target)

    val init = (baseTime, s.startAt(baseTime))
    val step2 = { (completed: Boolean, prev: s.Target, now: Instant) =>
      if (completed) s.completedAt(prev, now) else s.timedOutAt(prev, now)
    }

    completions.scanLeft(init) { case (previous, (delta, completed)) =>
      val now = previous._1 + delta
      (now, step2(completed, previous._2, now))
    }.map { case (time, target) =>
      MomentInfo(time, TargetInfo(target.initiateAt, target.timeoutAt))
    }
  }

}


