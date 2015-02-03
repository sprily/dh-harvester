package uk.co.sprily.dh
package harvester
package controllers

import scala.concurrent.duration._

import scalaz._
import Scalaz._

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions
import org.specs2.scalaz.ValidationMatchers

import network.DeviceId
import network.DeviceLike
import capture.RequestLike
import scheduling.Schedule

class InstanceManagerSpec extends SpecificationLike
                             with ValidationMatchers
                             with NoTimeConversions {

  import InstanceManager.Protocol._

  "An InstanceManager" should {

  }

  class Context extends AkkaSpecs2Support with ImplicitSender {
    def instanceManager() = TestActorRef(new InstanceManager() with DeviceManagerProvider {
      override def deviceManagerProps = ???
    })
  }
}
