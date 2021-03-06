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

import ScheduleSpec.traceExecution

class DelaySpec extends Specification with ScalaCheck
                                      with NoTimeConversions
                                      with CommonGenerators {

  implicit val ps = Parameters(minTestsOk=300)

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

          val delayedDeadline = delayedTarget.get.timeoutDelayFrom(baseTime)
          val normalDeadline  = normalTarget.get.timeoutDelayFrom(baseTime) + delay

          delayedDeadline must === (normalDeadline)
        }
      }
    }

    "delay of zero is equal to the underlying schedule" in {
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.all(sz)))
      implicit val completions = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      prop {
        (s: Schedule, completions: Seq[(FiniteDuration, Boolean)]) => {
          ScheduleSpec.equalTraces(s, s.delayBy(0.seconds))(completions)
        }
      }
    }

    "generate meaningful Targets" in {
      val FDs = FDGen.choose(0.seconds, 60.seconds)
      implicit val arbFDs = Arbitrary(FDs)
      implicit val primitives = ScheduleGen.primitives
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.delay(FDs, sz)))
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
