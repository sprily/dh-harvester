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

class EachSpec extends Specification with ScalaCheck
                                     with NoTimeConversions
                                     with CommonGenerators {

  implicit val ps = Parameters(minTestsOk=300)

  "Each" should {

    "target an immediate start" in {
      firstTarget.initialDelayFrom(baseTime) must === (Duration.Zero)
    }

    "timeout at the *next* target" in {
      firstTarget.timeoutDelayFrom(baseTime) must === (3.seconds)
    }

    "delay initialising the subsequent target" in {
      implicit val FD = Arbitrary(FDGen.choose(0.seconds, 3.seconds))
      prop {
        (timeToComplete: FiniteDuration) => {
          val target = schedule.completedAt(firstTarget, baseTime + timeToComplete)
          target.initialDelayFrom(baseTime) must === (3.seconds)
        }
      }
    }

    "delay the timeout of the subsequent target" in {
      implicit val FD = Arbitrary(FDGen.choose(0.seconds, 6.seconds))
      prop {
        (timeToComplete: FiniteDuration) => {
          (timeToComplete < 6.seconds) ==> {
            val target = schedule.completedAt(firstTarget, baseTime + timeToComplete)
            target.timeoutDelayFrom(baseTime) must === (6.seconds)
          }
        }
      }
    }

    "immediately re-schedule if the timeout is missed" in {
      implicit val FD = Arbitrary(FDGen.choose(3.seconds, 9.seconds))
      prop {
        (timeToComplete: FiniteDuration) => {
          val target = schedule.completedAt(firstTarget, baseTime + timeToComplete)
          target.initialDelayFrom(baseTime + timeToComplete) must === (Duration.Zero)
        }
      }
    }

    "timeout at the *next* available target" in {
      implicit val FD = Arbitrary(FDGen.choose(6.seconds, 9.seconds))
      prop {
        (timeToComplete: FiniteDuration) => {
          (timeToComplete < 9.seconds) ==> {
            val target = schedule.completedAt(firstTarget, baseTime + timeToComplete)
            target.timeoutDelayFrom(baseTime) must === (9.seconds)
          }
        }
      }
    }

    "generate meaningful Targets" in {
      implicit val FDs = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      implicit val schedules = Arbitrary(for {
        interval <- FDGen.choose(0.seconds, 10.seconds)
        if interval > Duration.Zero
      } yield Schedule.each(interval))
      ScheduleSpec.meaningfulTargetProperty
    }

  }

  private lazy val firstTarget = schedule.startAt(baseTime)
  private lazy val schedule = Schedule.each(3.seconds)
  private lazy val baseTime = Instant.now()
}
