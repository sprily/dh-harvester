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

class UnionSpec extends Specification with ScalaCheck
                                      with NoTimeConversions
                                      with CommonGenerators {

  "Union" should {

    "generate meaningful Targets" in {
      implicit val FDs = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      implicit val primitives = ScheduleGen.primitives
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.union(sz)))
      ScheduleSpec.meaningfulTargetProperty
    }.set(minTestsOk=100)

  }
}
