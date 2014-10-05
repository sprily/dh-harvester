package uk.co.sprily.dh
package harvester

import scala.concurrent.duration._

import org.joda.time.LocalDateTime

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import org.specs2.mutable.Specification
import org.specs2.ScalaCheck

class TargetSpec extends Specification with ScalaCheck {

  import Target._

  "Each() Target" should {

    "schedule an immediate start" in {
      val target = Each(3.seconds)
      val schedule = target.start(now=dt)
      schedule.delay must === (Duration.Zero)
    }

    "target a uniform distribution" in {
      val target = Each(3.seconds)
      val initSchedule = target.start(now=dt)
      val schedule = target.completed(initSchedule, now = dt + 1.seconds)
      schedule.delay must === (2.seconds)
    }

    "timeout at the next target-time" in {
      val target = Each(3.seconds)
      val initSchedule = target.start(now=dt)
      val schedule = target.completed(initSchedule, now = dt + 1.seconds)
      schedule.timeout must === (3.seconds)
    }

    "immediatly re-schedule if the target time is missed" in {
      val target = Each(3.seconds)
      val initSchedule = target.start(now=dt)
      val schedule = target.completed(initSchedule, now = dt + 4.seconds)
      schedule.delay must === (Duration.Zero)
    }

    "reduce timeout if the target time is missed" in {
      val target = Each(3.seconds)
      val initSchedule = target.start(now=dt)
      val schedule = target.completed(initSchedule, now = dt + 4.seconds)
      schedule.timeout must === (2.seconds)
    }

    "handle more than one missed target" in {
      val target = Each(3.seconds)
      val initSchedule = target.start(now=dt)
      val schedule = target.completed(initSchedule, now = dt + 7.seconds)
      schedule.timeout must === (2.seconds)
    }
  }


  "All Targets" should {

    "only issue non-negative delays" in {
      scheduleProp { schedule =>
        schedule.delay must beGreaterThanOrEqualTo (Duration.Zero)
      }
    }

    "only issue positive timeouts" in {
      scheduleProp { schedule =>
        schedule.timeout must beGreaterThan (Duration.Zero)
      }
    }


    "progress monotonically" in {
      prop {
        (interval: FiniteDuration, steps: Seq[FiniteDuration]) => {
          (interval > Duration.Zero) ==> {
            val target = Each(interval)
            val initSchedule = target.start(now=dt)
            val schedules = steps.scanLeft((initSchedule, dt)) { case ((accSch, now), step) =>
              (target.completed(accSch, now = now + step), now + step)
            }

            schedules.map { case (sch, dt) => dt + sch.timeout}.must(beSorted)
          }
        }
      }
    }

  }

  private implicit val LocalDateTimeOrdering = new Ordering[LocalDateTime] {
    def compare(t1: LocalDateTime, t2: LocalDateTime) = t1.compareTo(t2)
  }

  private def scheduleProp(f: ScheduleInfo => Boolean) = prop {
    (interval: FiniteDuration, completion: FiniteDuration) => {
      (interval > Duration.Zero) ==> {
        val target = Each(interval)
        val initSchedule = target.start(now=dt)
        val schedule = target.completed(initSchedule, now = dt + completion)
        f(schedule)
      }
    }
  }

  private implicit def positiveDurations = Arbitrary { 
    Gen.choose(0, Int.MaxValue) map (_.millis)
  }

  private lazy val dt = new LocalDateTime(2014, 9, 1, 13, 30, 4)

  /** Hides specs2.Specification's implicit Int conversion.
    * because we want the scala.concurrent.duration ones
    */
  override def intToRichLong(v: Int) = super.intToRichLong(v)
}
