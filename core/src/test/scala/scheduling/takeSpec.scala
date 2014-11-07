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

    "generate meaningful Targets" in {
      implicit val FDs = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      implicit val limits = Gen.choose[Long](0L, 10L)
      implicit val primitives = ScheduleGen.primitives
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.take(sz)))
      ScheduleSpec.meaningfulTargetProperty
    }

    "union(s,s) === s" in {
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.all(sz)))
      implicit val completions = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      prop {
        (s: Schedule,
         completions: Seq[(FiniteDuration, Boolean)]) => {
          ScheduleSpec.equalTraces(s, s.unionWith(s))(completions)
         }
      }
    }

    "union(s1, s2) === union(s2, s1)" in {
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.all(sz)))
      implicit val completions = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      prop {
        (s1: Schedule,
         s2: Schedule,
         completions: Seq[(FiniteDuration, Boolean)]) => {
           ScheduleSpec.equalTraces(s1, s2)(completions)
         }
      }
    }

    "union(each(2N), delay(each(2N), N)) === each(N)" in {
      implicit val durations = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      prop {
        (N: FiniteDuration,
         completions: Seq[(FiniteDuration, Boolean)]) => {
          import Schedule.each
          val union = each(2 * N).unionWith(each(2*N).delayBy(N))
          val nonUnion = each(N)
          ScheduleSpec.equalTraces(union, nonUnion)(completions)
        }
      }
    }

  }
}
