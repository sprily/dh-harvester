package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import org.specs2.mutable.Specification
import org.specs2.ScalaCheck
import org.specs2.matcher.Parameters

object ScheduleSpec extends Specification with ScalaCheck
                                          with HideDurationImplicits {

  def meaningfulTargetProperty(implicit schedules: Arbitrary[Schedule],
                                        completionTimes: Arbitrary[FiniteDuration]) = {
    prop {
      (s: Schedule, completionTimes: Seq[FiniteDuration]) => {

        val baseTime = Instant.now()
        val states = runSchedule(baseTime)(completionTimes)(s)

        type State = (s.Target, Instant)
        states must contain { state: State =>
          val (target, dt) = state
          (target.initialDelayFrom(dt) must beGreaterThanOrEqualTo(Duration.Zero)) and
          (target.timeoutDelayFrom(dt) must beGreaterThan(target.initialDelayFrom(dt)))
        }.foreach

      }
    }
  }

  def runSchedule[S <: Schedule]
                 (baseTime: Instant)
                 (completionTimes: Seq[FiniteDuration])
                 (s: S): Seq[(s.Target, Instant)] = {
    
    type State = (s.Target, Instant)
    val init: State = (s.startAt(baseTime), baseTime)

    val accumulate = { (prev: State, delta: FiniteDuration) =>
      val (prevTarget, prevDT) = prev
      val now = prevDT + delta
      (s.completedAt(prevTarget, now), now)
    }

    completionTimes.scanLeft(init)(accumulate)
  }

  def runScheduleNormalized(baseTime: Instant)
                           (completionTimes: Seq[FiniteDuration])
                           (s: Schedule): Seq[(TargetInfo, Instant)] = {
    type State = (s.Target, Instant)
    runSchedule(baseTime)(completionTimes)(s).map { case state: State =>
      val (target, dt) = state
      (TargetInfo(target.initiateAt, target.timeoutAt), dt)
    }
  }
}

case class TargetInfo(
    initialDelay: Instant,
    timeoutDelay: Instant)
