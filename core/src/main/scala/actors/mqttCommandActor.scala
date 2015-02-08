package uk.co.sprily.dh
package harvester
package actors

import scala.util.Try
import scala.util.Success
import scala.util.Failure

import scalaz._
import Scalaz._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy
import akka.actor.Terminated
import akka.util.ByteString

import spray.json._

import uk.co.sprily.mqtt.Cont
import uk.co.sprily.mqtt.ClientModule
import uk.co.sprily.mqtt.TopicPattern
import uk.co.sprily.mqtt.Topic
import uk.co.sprily.mqtt.MqttMessage

import api.JsonUtils

/** Models receiving commands via an MQTT broker.
 */
abstract class MqttCommandActor(
    root: TopicPattern,
    client: ClientModule[Cont]#Client) extends Actor
                                          with ActorLogging {

  import MqttCommandActor.Types._

  /** Abstract members **/
  type Command
  type Result
  protected def childProps(id: RequestId, c: Command): Props
  implicit def commandJson: JsonReader[Command]
  implicit def resultJson: JsonWriter[Result]

  /** Returned by children to communicate Result or some sort of error **/
  type Payload = \/[CommandError,Result]
  protected case class Response(id: RequestId, payload: Payload)
  protected object Response {
    def apply(r: Request, err: CommandError): Response = {
      Response(r.id, err.left)
    }

    def apply(r: Request, result: Result): Response = {
      Response(r.id, result.right)
    }
  }

  /** Private state **/
  private[this] var responses = Map.empty[RequestId, Response]
  private[this] var requests = Map.empty[ActorRef, Request]

  /** Actor Hooks **
    *
    *  - Perform broker subscription upon *first* initialisation only
    *  - Always stop a failing child actor, but capture the reason it
    *    failed before stopping it.
    */
  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception =>
      collect(Response(requests(sender), RequestError(e.getMessage)))
      SupervisorStrategy.Stop
  }

  override def postRestart(reason: Throwable): Unit = ()
  override def preStart(): Unit = {
    log.info(s"Initialising MqttCommandActor on root: $root")
    client.data(requestPattern) { msg =>
      log.info(s"Command message received: $msg")
      self ! Request(id = extractId(msg),
                     payload = ByteString(msg.payload.toArray))
    }
  }

  def receive = {

    case (r: Request) =>
      r.extract[Command] match {
        case Success(c) =>
          log.info(s"Received Command on '$root': $c")
          val child = context.actorOf(childProps(r.id, c), r.id.s)
          context.watch(child)
          requests = requests + (child -> r)
          child ! c
        case Failure(e) =>
          log.warning(s"Error extracting Command on '$root': $e")
          sendResponse(Response(r, RequestError(e.getMessage)))
          cleanup(r)
      }

    case (r: Response) =>
      log.info(s"Received Response to request ${r.id}")
      collect(r)
      context.stop(sender)

    case Terminated(child) =>
      log.info(s"$child terminated, cleaning up")
      context.unwatch(child)
      requests.get(child).foreach { request =>
        val response = responses.get(request.id).getOrElse(Response(request, InternalError))
        sendResponse(response)
        requests = requests - child
        responses = responses - request.id
        cleanup(request)
      }

  }

  private def sendResponse(r: Response) = {
    val payload = r.payload.toJson.prettyPrint.getBytes("UTF-8")
    client.publish(topic=responseTopic(r.id), payload=payload)
  }

  private def collect(r: Response) = {
    responses = responses + (r.id -> r)
  }

  /** Clean up after a Request by sending an empty payload to the
    * request's endpoint.
    */
  private def cleanup(r: Request): Unit = {
    client.publish(topic = requestTopic(r.id),
                   payload = Array.empty[Byte])
  }

  private def extractId(msg: MqttMessage): RequestId = {
    RequestId(msg.topic.path.split("/").init.last)
  }

  private def requestPattern: TopicPattern = {
    TopicPattern(root.path + "/commands/+/request")
  }

  private def requestTopic(id: RequestId): Topic = {
    Topic(root.path + s"/commands/${id.s}/request")
  }

  private def responseTopic(id: RequestId): Topic = {
    Topic(root.path + s"/commands/${id.s}/response")
  }

}

object MqttCommandActor extends JsonUtils {

  import Types.CommandError

  // Supporting types
  object Types {

    trait CommandError { def msg: String }
    case class RequestError(msg: String) extends CommandError
    case object InternalError extends CommandError { val msg = "Internal Error" }
    case object TimedOutError extends CommandError { val msg = "Timed out waiting for response." }

    case class RequestId(s: String)
    case class Request(id: RequestId, payload: ByteString) {

      final def json: Try[JsObject] = Try {
        payload.decodeString("UTF-8").parseJson.asJsObject
      }

      def extract[T: JsonReader] = for {
        js <- json
        t <- js.tryConvertTo[T]
      } yield t
    }
  }

  protected[MqttCommandActor] implicit def disjJson[A: JsonWriter, B: JsonWriter]
                                                    : JsonWriter[\/[A,B]] = {
    new JsonWriter[\/[A,B]] {
      def write(e: \/[A,B]) = e.fold(
        a => implicitly[JsonWriter[A]].write(a),
        b => implicitly[JsonWriter[B]].write(b)
      )
    }
  }

  protected[MqttCommandActor] implicit def errorJson: JsonWriter[CommandError] = {
    new JsonWriter[CommandError] {
      def write(e: CommandError) = JsObject("error" -> JsString(e.msg))
    }
  }

}
