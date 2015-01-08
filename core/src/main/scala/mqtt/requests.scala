package uk.co.sprily.dh
package harvester
package mqtt

import scala.language.higherKinds

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props

import spray.json._

import uk.co.sprily.mqtt.Cont
import uk.co.sprily.mqtt.ClientModule
import uk.co.sprily.mqtt.TopicPattern

import api.JsonFormats
import api.ManagedInstance

import modbus.ModbusDevice

class Requests(
    reqTopic: TopicPattern,
    persReqTopic: TopicPattern,
    client: ClientModule[Cont]#Client)
  extends Actor with ActorLogging {

  // akka hooks.
  // perform broker subscription upon *first* initialisation only
  override def postRestart(reason: Throwable): Unit = ()
  override def preStart(): Unit = {

    client.data(reqTopic) { msg =>
      log.info(s"RCVD: $msg")
    }

    client.data(persReqTopic) { msg =>
      import JsonFormats._
      import uk.co.sprily.dh.harvester.capture.RequestActorManager
      import RequestActorManager.Protocol._
      import scala.concurrent.duration._
      import scheduling.Schedule
      val json = new String(msg.payload.toArray, "UTF-8").parseJson.asJsObject
      val config = json.tryConvertTo[ManagedInstance].toOption.get    // TODO
      val reqs = config.devices.flatMap(_.scheduledRequests)
                               .map(ScheduledRequest.tupled)

      context.actorSelection("../request-manager") ! PersistentRequests(reqs)
    }

  }

  override def receive = {
    case _ => ()
  }

}

object Requests {
  def props(client: ClientModule[Cont]#Client) = Props(
    new Requests(reqPattern, pReqPattern, client)
  )

  def reqPattern = TopicPattern("test-org/todo")
  def pReqPattern = TopicPattern("test-org/todo/too")
}
