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

      implicit val schedules = Gen.lzy(all(depth-1))

      def nextSchedule() = Gen.frequency(
        4 -> primitives,
        4 -> delay(FDGen.choose(0.seconds, 10.seconds), depth-1),
        1 -> union(depth-1),
        4 -> fixedTimeout(FDGen.choose(0.seconds, 10.seconds), depth-1),
        4 -> retry(FDGen.choose(0.seconds, 120.seconds), depth-1)
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

    def fixedTimeout(timeouts: Gen[FiniteDuration], depth: Int)
                    (implicit others: Gen[Schedule]): Gen[Schedule] = {

      def nextSchedule(): Gen[Schedule] = Gen.frequency(
        1 -> Gen.lzy(fixedTimeout(timeouts, depth-1)),
        1 -> others
      )

      depth match {
        case 0 => others
        case _ => for {
          timeout <- timeouts
          sch   <- nextSchedule()
        } yield Schedule.fixedTimeout(sch, timeout)
      }
    }

    def retry(retries: Gen[FiniteDuration], depth: Int)
             (implicit others: Gen[Schedule]): Gen[Schedule] = {

      def nextSchedule(): Gen[Schedule] = Gen.frequency(
        1 -> Gen.lzy(retry(retries, depth-1)),
        1 -> others
      )

      depth match {
        case 0 => others
        case _ => for {
          retry <- retries
          sch   <- nextSchedule()
        } yield Schedule.retry(sch, retry)
      }
    }

    def take(depth: Int)
            (implicit others: Gen[Schedule], limits: Gen[Long]): Gen[Schedule] = {
      def nextSchedule() = Gen.frequency(
        1 -> Gen.lzy(take(depth-1)),
        1 -> others
      )

      depth match {
        case 0 => others
        case _ => for {
          s <- nextSchedule()
          limit <- limits
        } yield Schedule.take(s, limit)
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
