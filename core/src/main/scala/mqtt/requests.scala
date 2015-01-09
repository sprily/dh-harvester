package uk.co.sprily.dh
package harvester
package mqtt

import scala.language.higherKinds
import scala.util.Try
import scala.util.Success
import scala.util.Failure

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.util.ByteString

import spray.json._

import uk.co.sprily.mqtt.Cont
import uk.co.sprily.mqtt.ClientModule
import uk.co.sprily.mqtt.TopicPattern
import uk.co.sprily.mqtt.Topic

import api.JsonFormats
import api.JsonFormats._
import api.ManagedDevice
import api.ManagedRequests

import capture.RequestActorManager.Protocol._

import modbus.ModbusDevice

class Requests(
    reqTopic: TopicPattern,
    persReqTopic: TopicPattern,
    client: ClientModule[Cont]#Client)
  extends Actor with ActorLogging {

  import Requests.Protocol._

  // akka hooks.
  // perform broker subscription upon *first* initialisation only
  override def postRestart(reason: Throwable): Unit = ()
  override def preStart(): Unit = {

    client.data(reqTopic) { msg =>
      log.debug("Adhoc request received")
      self ! AdHoc(ByteString(msg.payload.toArray))
    }

    client.data(persReqTopic) { msg =>
      log.debug("Persistent request received")
      self ! Persistent(ByteString(msg.payload.toArray))
    }

  }

  override def receive = {

    case _ => ()

    //case p@Persistent(_) => p.extract[ManagedInstance] match {
    //  case Success(config) => requestManager ! extractRequests(config)
    //  case Failure(e)      => log.error(s"Error processing Persistent command: $e")
    //}

    //case a@AdHoc(_) => a.extract[ManagedDevice] match {
    //  case Success(req) => requestManager ! req
    //  case Failure(e)   => log.error(s"Error processing AdHoc command: $e")
    //}

  }

  //private def extractRequests(config: ManagedInstance) = {
  //  PersistentRequests(
  //    config.devices.flatMap(_.scheduledRequests)
  //                  .map(ScheduledRequest.tupled)
  //  )
  //}

  private def requestManager = context.actorSelection("../request-manager")

}

object Requests {

  object Protocol {

    trait JsonPayload {
      def payload: ByteString
      def json: Try[JsObject] = Try {
        payload.decodeString("UTF-8").parseJson.asJsObject
      }
      def extract[T: JsonReader] = for {
        js <- json
        t <- js.tryConvertTo[T]
      } yield t
    }

    case class Persistent(payload: ByteString) extends JsonPayload
    case class AdHoc(payload: ByteString) extends JsonPayload
  }

  def props(root: Topic, client: ClientModule[Cont]#Client) = Props(
    new Requests(reqPattern(root), pReqPattern(root), client)
  )

  private implicit class TopicPatternOps(t: TopicPattern) {
    def ::(other: Topic): TopicPattern = {
      TopicPattern((other.path.split("/").toList ++ t.path.split("/").toList).mkString("/"))
    }
  }

  private def reqPattern(root: Topic) = root :: TopicPattern("manage/requests/adhoc")
  private def pReqPattern(root: Topic) = root :: TopicPattern("manage/requests/persistent")
}
