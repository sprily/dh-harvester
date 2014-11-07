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

class TakeSpec extends Specification with ScalaCheck
                                     with NoTimeConversions
                                     with CommonGenerators {

  implicit val ps = Parameters(minTestsOk=300)

  "Take" should {

    "stop providing Targets when limit is hit" in {

      import ScheduleSpec.TargetInfo

      implicit val schedules = Arbitrary(ScheduleGen.primitives)
      implicit val limits = Arbitrary(Gen.choose(1,10))
      implicit val FDs = Arbitrary(FDGen.choose(0.seconds, 60.seconds))

      prop {
        (s: Schedule,
         limit: Int,
         completions: Seq[(FiniteDuration, Boolean)]) => {
          val limited = s.take(limit)
          val trace = ScheduleSpec.traceExecution(Instant.now())(completions)(limited)
          val targets = trace.map(_.target)
          val (expectedSome, expectedNone) = targets.splitAt(limit)

          (expectedSome must contain(beSome[TargetInfo]).forall) and
          (expectedNone must contain(beNone).forall)
        }
      }
    }

    "generate meaningful Targets" in {
      implicit val FDs = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      implicit val limits = Gen.choose[Long](0L, 10L)
      implicit val primitives = ScheduleGen.primitives
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.take(sz)))
      ScheduleSpec.meaningfulTargetProperty
    }

    "take(s, N) should match trace of s up until N" in {
      implicit val schdules = Arbitrary(Gen.sized(ScheduleGen.all(_)))
      implicit val completions = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      implicit val limits = Arbitrary(Gen.choose(1, 10))
      prop {
        (s: Schedule,
         completions: Seq[(FiniteDuration, Boolean)],
         N: Int) => {
          val limitedCompletions = completions.take(N-1)
          ScheduleSpec.equalTraces(s, s.take(N))(limitedCompletions)
        }

      }
    }
  }
}
