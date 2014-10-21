package uk.co.sprily.dh
package harvester
package scheduling

import scala.concurrent.duration.Deadline
import scala.concurrent.duration.FiniteDuration

trait DeadlineExtras {
  implicit class DeadlineOps(d: Deadline) {

    def min(d2: Deadline) = if (d <= d2) d else d2

    protected[scheduling] def timeLeftAt(now: Instant): FiniteDuration = d.time - now.time
  }
}
