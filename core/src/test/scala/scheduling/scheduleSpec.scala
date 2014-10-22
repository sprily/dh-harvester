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
      (s: Schedule, completionTimes: Seq[FiniteDuration]) => {

        val baseTime = Instant.now()
        val trace = traceExecutionInfo(baseTime)(completionTimes)(s)

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
                    (completionTimes: Seq[FiniteDuration])
                    (s: S): Seq[Moment[s.Target]] = {

    val init = Moment(baseTime, s.startAt(baseTime))

    val accumulate = { (prev: Moment[s.Target], delta: FiniteDuration) =>
      val now = prev.time + delta
      Moment(now, s.completedAt(prev.target, now))
    }

    completionTimes.scanLeft(init)(accumulate)
  }

  def traceExecutionInfo(baseTime: Instant)
                        (completionTimes: Seq[FiniteDuration])
                        (s: Schedule): Seq[MomentInfo] = {
    traceExecution(baseTime)(completionTimes)(s).map { moment =>
      MomentInfo(moment.time, TargetInfo(moment.target.initiateAt,
                                         moment.target.timeoutAt))
    }
  }
}


