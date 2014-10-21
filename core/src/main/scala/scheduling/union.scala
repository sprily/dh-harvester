package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

case class Union(s1: Schedule, s2: Schedule) extends Schedule {

  case class Target(
      val initiateAt: Deadline,
      val timeoutAt: Deadline,
      val targets: (s1.Target, s2.Target),
      val chosen: Or[s1.Target, s2.Target]) extends TargetLike

  override def startAt(now: Instant) = {
    val t1 = s1.startAt(now)
    val t2 = s2.startAt(now)
    target(t1,t2)
  }

  override def completedAt(previous: Target, now: Instant) = {

    val (t1, t2) = previous.chosen match {
      case Fst(t1) =>
        val newT1 = s1.completedAt(t1, now)
        val t2 = previous.targets._2
        val newT2 = t2.initiateAt - now match {
          case delta if delta > Duration.Zero => t2
          case delta                          => s2.completedAt(t2, now)
        }
        (newT1, newT2)

      case Snd(t2) =>
        val newT2 = s2.completedAt(t2, now)
        val t1 = previous.targets._1
        val newT1 = t1.initiateAt - now match {
          case delta if delta > Duration.Zero => t1
          case delta                          => s1.completedAt(t1, now)
        }
        (newT1, newT2)

      case Both(t1,t2) =>
        (s1.completedAt(t1, now),
         s2.completedAt(t2, now))
    }

    target(t1,t2)
  }

  override def timedOutAt(previous: Target, now: Instant) = ???

  private def target(t1: s1.Target, t2: s2.Target) = {

    val initiateAt = t1.initiateAt min t2.initiateAt
    val targets    = (t1, t2)

    val (timeoutAt, chosen) = (t1.initiateAt, t2.initiateAt) match {

      case (init1, init2) if init1 < init2 =>
        (t1.timeoutAt min t2.initiateAt, Fst(t1))

      case (init1, init2) if init1 > init2 =>
        (t2.timeoutAt min t1.initiateAt, Snd(t2))

      case (init1, init2) if init1 == init2 =>
        (t1.timeoutAt min t2.timeoutAt, Both(t1, t2))
    }

    Target(initiateAt, timeoutAt, targets, chosen)
  }
}

/** Non-exclusive disjunction **/
sealed trait Or[+T1, +T2]
case class Fst[T](t: T) extends Or[T,Nothing]
case class Snd[T](t: T) extends Or[Nothing,T]
case class Both[T1,T2](t1: T1, t2: T2) extends Or[T1,T2]
