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
import ScheduleSpec.successOnlyStep
import ScheduleSpec.traceExecutionInfo

class RetrySpec extends Specification with ScalaCheck
                                     with NoTimeConversions
                                     with CommonGenerators {

  implicit val ps = Parameters(minTestsOk=300)

  "Retry" should {

    "not deviate from underlying schedule when successful" in {
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.all(sz)))
      implicit val completions = Arbitrary(FDGen.choose(0.seconds, 60.seconds))
      prop {
        (s: Schedule, 
         retry: FiniteDuration,
         completionTimes: Seq[FiniteDuration]) => {
          val retrySch = s.retryEvery(retry)
          val now = Instant.now()
          val completions = completionTimes.map((_,true))

          val retryStep = successOnlyStep(retrySch) _
          val step = successOnlyStep(s) _

          val withoutRetry = traceExecutionInfo(now)(completions)(s)(step)
          val withRetry    = traceExecutionInfo(now)(completions)(retrySch)(retryStep)

          withoutRetry must === (withRetry)
        }
      }
    }

    "retry low-frequency schedules" in {
      val underTest = Schedule.each(1.hour)
                              .fixTimeoutTo(1.minute)
                              .retryEvery(5.minutes)

      val now = Instant.now()
      val completions = List(
        (1.minute, false),  // 1st attempt times-out at 1 minute
        (5.minute, false),  // 2nd attempt times-out at 6 minutes
        (30.seconds, true)  // 3rd attempt succeeds at 6min 30s.
      )

      val trace = traceExecutionInfo(now)(completions)(underTest)(chooseStep(underTest))

      val initiateDeadlines = trace.map(_.target.initiateAt)
      initiateDeadlines must === (List(
        now + 0.seconds,
        now + 5.minute,
        now + 10.minutes,
        now + 1.hour
      ))
    }

    "retry high-frequency schedule" in {
      val underTest = Schedule.each(1.second)
                              .fixTimeoutTo(10.seconds)
                              .retryEvery(15.seconds)

      val now = Instant.now()
      val completions = List(
        (10.seconds, false),  // 1st attempt times-out at 10 seconds
        (15.seconds, false),  // 2nd attempt times-out at 25 seconds
        (5300.millis, true)   // 3rd attempt successed at 30.3 seconds
      )

      val trace = traceExecutionInfo(now)(completions)(underTest)(chooseStep(underTest))

      val initiateDeadlines = trace.map(_.target.initiateAt)
      initiateDeadlines must === (List(
        now + 0.seconds,
        now + 15.seconds,
        now + 30.seconds,
        now + 30.seconds + 300.millis
      ))
    }

    "generate meaningful Targets" in {
      val FDs = FDGen.choose(0.seconds, 60.seconds)
      implicit val arbFDs = Arbitrary(FDs)
      implicit val primitives = ScheduleGen.primitives
      implicit val schedules = Arbitrary(Gen.sized(sz => ScheduleGen.retry(FDs, sz)))
      ScheduleSpec.meaningfulTargetProperty
    }

  }

}
