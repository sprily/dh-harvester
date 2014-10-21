package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration._

case class Union(s1: Schedule, s2: Schedule) extends Schedule {

  case class Target(
      val initiateAt: Deadline,
      val timeoutAt: Deadline,
      val targets: (s1.Target, s2.Target),
      val chosen: (Option[s1.Target], Option[s2.Target])) extends TargetLike {

    require(chosen._1 != None || chosen._2 != None)
  }

  override def startAt(now: Instant) = {
    val t1 = s1.startAt(now)
    val t2 = s2.startAt(now)
    target(t1,t2)
  }

  override def completedAt(previous: Target, now: Instant) = {

    val (t1, t2) = previous.chosen match {
      case (Some(t1), None) =>
        val newT1 = s1.completedAt(t1, now)
        val t2 = previous.targets._2
        val newT2 = t2.initiateAt - now match {
          case delta if delta > Duration.Zero => t2
          case delta                          => s2.completedAt(t2, now)
        }
        (newT1, newT2)

      case (None, Some(t2)) =>
        val newT2 = s2.completedAt(t2, now)
        val t1 = previous.targets._1
        val newT1 = t1.initiateAt - now match {
          case delta if delta > Duration.Zero => t1
          case delta                          => s1.completedAt(t1, now)
        }
        (newT1, newT2)

      case (Some(t1), Some(t2)) =>
        (s1.completedAt(t1, now),
         s2.completedAt(t2, now))
    }

    target(t1,t2)
  }

  override def timedOutAt(previous: Target, now: Instant) = ???

  private def target(t1: s1.Target, t2: s2.Target) = (t1.initiateAt, t2.initiateAt) match {
    case (init1, init2) if init1 < init2 =>
      Target(
        initiateAt = t1.initiateAt,
        timeoutAt  = t1.timeoutAt min t2.initiateAt,
        targets    = (t1, t2),
        chosen     = (Some(t1), None))
    case (init1, init2) if init1 > init2 =>
      Target(
        initiateAt = t2.initiateAt,
        timeoutAt  = t2.timeoutAt min t1.initiateAt,
        targets    = (t1, t2),
        chosen     = (None, Some(t2)))
    case (init1, init2) if init1 == init2 =>
      Target(
        initiateAt = t1.initiateAt,
        timeoutAt  = t1.timeoutAt min t2.timeoutAt,
        targets    = (t1, t2),
        chosen     = (Some(t1), Some(t2)))
  }
}
