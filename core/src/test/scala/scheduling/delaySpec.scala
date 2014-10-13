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

class DelaySpec extends Specification with ScalaCheck
                                      with HideDurationImplicits
                                      with CommonGenerators {

  "Delay" should {

    "target the intended delayed start" in {
      firstTarget.initialDelayFrom(baseTime) must === (delay)
    }

    "timeout at the *next* target" in {
      firstTarget.timeoutDelayFrom(baseTime) must === (delay + eachInterval)
    }

    "subsequent target is shifted" in {
      implicit val FD = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      prop {
        (timeToComplete: FiniteDuration) => {
          val delayedTarget = schedule.completedAt(firstTarget, baseTime + timeToComplete + delay)
          val normalTarget = each.completedAt(each.startAt(baseTime), baseTime + timeToComplete)

          val delayedDeadline = delayedTarget.timeoutDelayFrom(baseTime)
          val normalDeadline  = normalTarget.timeoutDelayFrom(baseTime) + delay

          delayedDeadline must === (normalDeadline)
        }
      }
    }

    "delay of zero is equal to the underlying schedule" in {
      implicit val schedules = Arbitrary(ScheduleGen.schedules)
      implicit val completions = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      prop {
        (s: Schedule, completionTimes: Seq[FiniteDuration]) => {
          val delayedSchedule = s.delayBy(0.seconds)
          val runner = ScheduleSpec.runScheduleNormalized(Instant.now())(completionTimes) _
          val withoutDelay = runner(s)
          val withDelay    = runner(delayedSchedule)

          withoutDelay must === (withDelay)
        }
      }
    }

    "generate meaningful Targets" in {
      implicit val FDs: Gen[FiniteDuration] = FDGen.choose(0.seconds, 60.seconds)
      implicit val schedules = ScheduleGen.delay(FDs, ScheduleGen.schedules)
      ScheduleSpec.meaningfulTargetProperty
    }

  }

  private lazy val eachInterval = 3.seconds
  private lazy val each = Schedule.each(eachInterval)
  private lazy val delay = 5.seconds
  private lazy val firstTarget = schedule.startAt(baseTime)
  private lazy val schedule = Schedule.delay(each, delay)
  private lazy val baseTime = Instant.now()
}
