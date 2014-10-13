package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

trait CommonGenerators {

  object FDGen {
    def choose(min: FiniteDuration,
               max: FiniteDuration): Gen[FiniteDuration] = {
      Gen.choose(min.toMillis, max.toMillis) map (_.millis)
    }
  }

  object ScheduleGen {

    def schedules: Gen[Schedule] = Gen.oneOf(
      each(FDGen.choose(0.seconds, 10.seconds)),
      delay(FDGen.choose(0.seconds, 10.seconds),
            Gen.lzy(schedules))
    )

    def each(intervals: Gen[FiniteDuration]): Gen[Schedule] = for {
      interval <- intervals
      if interval > Duration.Zero
    } yield Schedule.each(interval)

    def delay(delays: Gen[FiniteDuration],
              schedulers: Gen[Schedule]): Gen[Schedule] = for {
      delay <- delays
      scheduler <- schedulers
    } yield Schedule.delay(scheduler, delay)

  }

}
