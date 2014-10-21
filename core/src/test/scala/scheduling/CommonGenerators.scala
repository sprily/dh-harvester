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

    def primitives = each(FDGen.choose(0.seconds, 60.seconds))

    def all(depth: Int): Gen[Schedule] = {

      def nextSchedule() = Gen.frequency(
        4 -> primitives,
        4 -> delay(FDGen.choose(0.seconds, 10.seconds), depth-1)(Gen.lzy(all(depth-1))),
        1 -> union(depth-1)(Gen.lzy(all(depth-1)))
      )

      if (depth <= 0) primitives else nextSchedule()
    }

    def delay(delays: Gen[FiniteDuration], depth: Int)
             (implicit others: Gen[Schedule]): Gen[Schedule] = {

      def nextSchedule(): Gen[Schedule] = Gen.frequency(
        1 -> Gen.lzy(delay(delays, depth-1)),
        1 -> others
      )

      depth match {
        case 0 => others
        case _ => for {
          delay <- delays
          sch   <- nextSchedule()
        } yield Schedule.delay(sch, delay)
      }
    }

    def union(depth: Int)
             (implicit others: Gen[Schedule]): Gen[Schedule] = {

      def nextSchedule() = Gen.frequency(
        1  -> Gen.lzy(union(depth-1)),
        4  -> others
      )

      depth match {
        case 0 => others
        case _ => for {
          left <- nextSchedule()
          right <- nextSchedule()
        } yield Schedule.union(left, right)
      }
    }

    def each(intervals: Gen[FiniteDuration]): Gen[Schedule] = for {
      interval <- intervals
      if interval > Duration.Zero
    } yield Schedule.each(interval)

  }

}