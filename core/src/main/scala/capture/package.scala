package uk.co.sprily.dh
package harvester

import scheduling.Schedule

package object capture {
  type ScheduledRequestLike = (RequestLike, Schedule)
}
