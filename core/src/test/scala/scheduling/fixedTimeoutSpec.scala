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

import ScheduleSpec.traceExecutionInfo
import ScheduleSpec.chooseStep

class FixedTimeoutSpec extends Specification with ScalaCheck
                                             with NoTimeConversions
                                             with CommonGenerators {

  "FixedTimeout" should {

    "fix timeout after initiateAt" in {
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.all(sz)))
      implicit val completions = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      prop {
        (s: Schedule,
         timeout: FiniteDuration,
         completions: Seq[(FiniteDuration, Boolean)]) => {
          val fixed = s.fixTimeoutTo(timeout)
          val now = Instant.now()
          val step = chooseStep(fixed) _
          val trace = traceExecutionInfo(now)(completions)(fixed)(step)

          import ScheduleSpec.MomentInfo
          trace must contain { (m: MomentInfo) =>
            (m.measuredDelay + timeout) must === (m.measuredTimeout)
          }.forall
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
}
