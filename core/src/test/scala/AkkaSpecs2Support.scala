package uk.co.sprily.dh
package harvester

import akka.actor.ActorSystem
import akka.testkit.TestKit

import org.specs2.mutable.After

/**
  * NOTE - if sub-classing this, don't put anything in the body
  * of the constructor of the sub-class.  `After` subclasses
  * `DelayedInit`, and the way `DelayedInit` works is to call
  * `delayedInit()` after *each* constructor in the class-hierarchy.
  * The outcome of this is that the actor system will be shutdown
  * before the test even starts.
  *
  * methods and lazy vals are OK in the body, just not vals or vars.
  */
class AkkaSpecs2Support extends TestKit(ActorSystem("test-system"))
                           with After {
  override def after = {
    system.shutdown()
  }
}
