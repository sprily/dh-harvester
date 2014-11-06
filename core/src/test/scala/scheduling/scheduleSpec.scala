package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import org.specs2.matcher.Matcher
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
          (m.measuredDelay must beNoneOr(beGreaterThanOrEqualTo(Duration.Zero))) and
          (m.measuredTimeout must beNoneOr(beGreaterThan(m.measuredDelay.get)))
        }.foreach

      }
    }
  }

  def beNoneOr[T](m: => Matcher[T]): Matcher[Option[T]] = {
    beNone or ((_: Option[T]).get) ^^ m
  }

  def equalTraces(s1: Schedule, s2: Schedule)
                 (implicit completions: Seq[(FiniteDuration, Boolean)]) = {

    val now = Instant.now()

    val trace1 = traceExecution(now)(completions)(s1)
    val trace2 = traceExecution(now)(completions)(s1)

    trace1 must === (trace2)
  }

  case class MomentInfo(time: Instant, target: Option[TargetInfo]) {
    val measuredDelay = target.map(_.initialDelayFrom(time))
    val measuredTimeout = target.map(_.timeoutDelayFrom(time))
  }

  case class TargetInfo(
      val initiateAt: Deadline,
      val timeoutAt: Deadline) extends TargetLike

  object TargetInfo {
    def apply(t: TargetLike): TargetInfo = TargetInfo(
      initiateAt = t.initiateAt,
      timeoutAt = t.timeoutAt)
  }

  def traceExecution[S <: Schedule]
                    (baseTime: Instant)
                    (completions: Seq[(FiniteDuration, Boolean)])
                    (s: S): Seq[MomentInfo] = {

    case class Moment(t: Instant, target: Option[s.Target])

    def step(completed: Boolean, prev: s.Target, now: Instant): Option[s.Target] = {
      if (completed) s.completedAt(prev, now) else s.timedOutAt(prev, now)
    }

    val init = Moment(baseTime, Some(s.startAt(baseTime)))
    completions.scanLeft(init) { case (prevMoment, (delta, completed)) =>
      val now = prevMoment.t + delta
      Moment(
        now,
        prevMoment.target.flatMap(step(completed, _, now))
      )
    }.map { m =>
      MomentInfo(m.t, m.target.map(TargetInfo.apply _))
    }
  }

}


