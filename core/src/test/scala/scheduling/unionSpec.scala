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

import ScheduleSpec.chooseStep
import ScheduleSpec.traceExecutionInfo

class UnionSpec extends Specification with ScalaCheck
                                      with NoTimeConversions
                                      with CommonGenerators {

  implicit val ps = Parameters(minTestsOk=300)

  "Union" should {

    "generate meaningful Targets" in {
      implicit val FDs = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      implicit val primitives = ScheduleGen.primitives
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.union(sz)))
      ScheduleSpec.meaningfulTargetProperty
    }.set(minTestsOk=100)

    "union(s,s) === s" in {
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.all(sz)))
      implicit val completions = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      prop {
        (s: Schedule,
         completions: Seq[(FiniteDuration, Boolean)]) => {
          val uS = s.unionWith(s)
          val now = Instant.now()

          val step = chooseStep(s) _
          val uStep = chooseStep(uS) _

          val trace = traceExecutionInfo(now)(completions)(s)(step)
          val uTrace = traceExecutionInfo(now)(completions)(uS)(uStep)

          trace must === (uTrace)
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
          val union1 = s1.unionWith(s2)
          val union2 = s2.unionWith(s1)
          val now = Instant.now()

          val trace1 = traceExecutionInfo(now)(completions)(union1)(chooseStep(union1))
          val trace2 = traceExecutionInfo(now)(completions)(union2)(chooseStep(union2))

          trace1 must === (trace2)
         }
      }
    }

  }
}
