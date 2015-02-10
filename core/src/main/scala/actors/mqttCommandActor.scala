package uk.co.sprily.dh
package harvester
package actors

import scala.reflect.runtime.universe._

import scala.concurrent.duration._

import scala.util.Try
import scala.util.Success
import scala.util.Failure

import scalaz._
import Scalaz._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.OneForOneStrategy
import akka.actor.ReceiveTimeout
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


/** Encapsulates a *single* request.  Responsible for:
  *
  *  - creating the work actor
  *  - timing out the request if the worker takes too long
  *  - writing a single response to the broker
  */
protected[actors] class RequestActor[Command:JsonReader, Result:JsonWriter:TypeTag](
    workerProps: Props,
    client: ClientModule[Cont]#Client,
    requestRoot: Topic,
    timeout: FiniteDuration) extends Actor with ActorLogging {

  import context.become
  import ApiEndpoint.Types._
  import ApiEndpoint.Protocol._
  import ApiEndpoint.Implicits._
  import RequestActor._

  /** Private state **/
  private[this] var request: Option[RawRequest] = None

  /** Constructor **/
  context.setReceiveTimeout(timeout)

  /** Akka hooks
    *
    *  - any exception in the worker triggers an InternalError
    *    response to be written to the broker's reponse topic,
    *    and this RequestActor requests to be stopped.
    *
    *  - Once stopped, for whatever reason, the RequestActor
    *    writes an empty body to the request topic.
    */
  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception =>
      log.error(s"Exception in worker: $e")
      write(InternalError.left)
      expire()
      become(awaitingTermination)
      SupervisorStrategy.Stop
  }
  override def postStop() = cleanup()

  def receive = {

    case (raw: RawRequest) =>
      request = Some(raw)
      raw.extract[Command] match {
        case Success(cmd) =>
          log.info(s"Received Command $cmd")
          issue(cmd)
          become(awaitingResult)
        case Failure(e) =>
          log.warning(s"Failed to extract Command: $e")
          write(RequestError(s"Failed to extract command: $e").left)
          expire()
          become(awaitingTermination)
      }

    case ReceiveTimeout =>
      val msg = s"Timed-out awaiting initial RawRequest from parent ($timeout)"
      log.error(msg)
      throw new RuntimeException(msg)
  }

  private def awaitingResult: Receive = {

    case Response(body, ev) if ev.tpe <:< typeOf[Result] =>
      log.info(s"Response received from child")
      write(body.asInstanceOf[ResponseBody[Result]])
      expire()
      become(awaitingTermination)
    case ReceiveTimeout =>
      log.warning(s"Timed-out awaiting Response from child")
      write(TimedOutError.left)
      expire()
      become(awaitingTermination)
    case (r: RawRequest) if Some(r) == request =>
      log.warning(s"Received a matching duplicate RawRequest.  Safe to ignore")
    case (r: RawRequest) =>
      log.warning(s"Received a conflicting duplicate RawRequest.")
      write(RequestError(s"Conflicting request: '${r.id.s}'").left)
      expire()
      become(awaitingTermination)
  }

  private def awaitingTermination: Receive = {
    case _ => {}
  }

  /** Issue the Command to a child **/
  private def issue(c: Command): Unit = {
    val worker = context.actorOf(workerProps, "worker")
    worker ! c
  }

  /** Notify the parent that we want to terminate **/
  private def expire(): Unit = {
    request match {
      case None =>
        throw new IllegalStateException("Unable to Expire, as no RawRequest received")
      case Some(raw) =>
        context.parent ! Expire(raw)
    }
  }

  /** write a response to the broker **/
  private def write(body: ResponseBody[Result]): Unit = {
    val payload = body.toJson.prettyPrint.getBytes("UTF-8")
    client.publish(
      topic=responseTopic,
      payload=payload)
  }

  /** Write an empty body to the request topic **/
  private def cleanup(): Unit = {
    client.publish(
      topic=requestTopic, payload=Array.empty[Byte])
  }

  private def requestTopic = Topic(requestRoot.path + "/request")
  private def responseTopic = Topic(requestRoot.path + "/response")

}

protected[actors] object RequestActor {
  def props[Command:JsonReader, Result:JsonWriter:TypeTag]
           (workerProps: Props,
            client: ClientModule[Cont]#Client,
            requestRoot: Topic)
           (implicit timeout: FiniteDuration) = {
    Props(new RequestActor[Command,Result](workerProps, client, requestRoot, timeout))
  }
}

abstract class ApiEndpoint(
    root: Topic,
    client: ClientModule[Cont]#Client) extends Actor
                                          with ActorLogging {

  import ApiEndpoint.Protocol._
  import ApiEndpoint.Types._

  /** Abstract members */
  type Command
  type Result
  implicit def commandReader: JsonReader[Command]
  implicit def resultWriter: JsonWriter[Result]
  implicit def resultTypeTag: TypeTag[Result]
  implicit val timeout: FiniteDuration
  protected val workerProps: Props

  private sealed trait ChildStatus
  private case class Active(child: ActorRef) extends ChildStatus
  private case class Terminating(child: ActorRef) extends ChildStatus

  /** private state **/
  private[this] var requests = Map.empty[RequestId, ActorRef]
  private[this] var terminating = Map.empty[RequestId, ActorRef]
  private[this] var redeliver = Map.empty[ActorRef, List[RawRequest]]

  /** Actor Hooks **
    *
    *  - Perform broker subscription upon *first* initialisation only.
    */
  override def postRestart(reason: Throwable): Unit = ()
  override def preStart(): Unit = {
    log.info(s"Initialising API endpoint on: $root")
    client.data(requestPattern) { msg =>
      if (msg.payload.nonEmpty) {
        val raw = RawRequest(id=extractId(msg),
                             payload=ByteString(msg.payload.toArray))
        log.info(s"RawRequest received on $root: $raw.id")
        self ! raw
      }
    }
  }

  def receive = {

    // RawRequest straight from the broker
    case (raw: RawRequest) =>
      getOrCreateRequestActor(raw) match {
        case Active(child)      => child ! raw
        case Terminating(child) =>
          log.warning(s"RawRequest matched to terminating child.  Re-delivering later: $raw")
          redeliver = redeliver |+| Map(child -> List(raw))
      }

    // Message from a child asking to be stopped
    case Expire(raw) =>
      val child = sender
      context.watch(child)  //  because stopping is asynchronous
      context.stop(child)
      terminating = terminating + (raw.id -> child)
      requests = requests - raw.id

    // Child has actually terminated
    case Terminated(child) =>
      context.unwatch(child)
      terminating.find { case (_,ref) => ref == child } match {

        case None =>  // no Expire message sent from child
          log.warning(s"RequestActor child terminated abnormally: $child")
          // expensive tidy up of requests
          requests = requests.filter { case (_, ref) => ref != child }

        case Some((id, ref)) =>
          val reqs = redeliver.getOrElse(child, Nil)
          redeliver = redeliver - child
          terminating = terminating - id
          reqs.foreach { self ! _ }
      }
  }

  /** Smart constructor to allow implementations easily construct Responses
    * of the correct type.
    */
  def response(body: ResponseBody[Result]): Response[Result] = {
    Response(body, resultTypeTag)
  }

  private def getOrCreateRequestActor(raw: RawRequest) = {
    (requests.get(raw.id), terminating.get(raw.id)) match {
      case (Some(child), None)       => Active(child)
      case (None, Some(terminating)) => Terminating(terminating)
      case (None, None)              => Active(spawnRequestActor(raw))
      case _  =>
        throw new IllegalStateException(
          s"Child is both active *and* terminating: ${raw.id}")
    }
  }

  private def spawnRequestActor(raw: RawRequest) = {
    log.info(s"Spawning new actor for $raw")
    val child = context.actorOf(
      RequestActor.props[Command,Result](
        workerProps,
        client,
        requestRoot(raw.id)),
      raw.id.s)
    requests = requests + (raw.id -> child)
    child
  }

  /** Extract the RequestId from the given MqttMessage topic **/
  private def extractId(msg: MqttMessage) = {
    RequestId(msg.topic.path.split("/").init.last)
  }

  /** The TopicPattern to subscribe to to receive requests **/
  private def requestPattern = TopicPattern(root.path + "/api/+/request")

  /** The Topic to publish responses to **/
  private def responseTopic(id: RequestId) = {
    Topic(root.path + s"/api/${id.s}/response")
  }

  /** The Topic that roots the given request id **/
  private def requestRoot(id: RequestId) = {
    Topic(root.path + s"/api/${id.s}")
  }
}

object ApiEndpoint extends JsonUtils {

  object Types {

    case class RequestId(s: String)
    type ResponseBody[R] = \/[CommandError,R]
    case class Response[R] protected[ApiEndpoint] (body: ResponseBody[R], ev: TypeTag[R])

    trait CommandError { def msg: String }
    case class RequestError(msg: String) extends CommandError
    case object InternalError extends CommandError { val msg = "Internal Error" }
    case object TimedOutError extends CommandError { val msg = "Timed out waiting for response." }
  }

  object Protocol {
    import Types._
    case class Expire(raw: RawRequest)
    case class RawRequest(id: RequestId, payload: ByteString) {

      def extract[T:JsonReader] = for {
        js <- json
        t  <- js.tryConvertTo[T]
      } yield t

      private def json = Try {
        payload.decodeString("UTF-8").parseJson.asJsObject
      }

    }
  }

  object Implicits {
    import Types._
    implicit def disjWriter[A: JsonWriter, B: JsonWriter]
                         : JsonWriter[\/[A,B]] = {
      new JsonWriter[\/[A,B]] {
        def write(e: \/[A,B]) = e.fold(
          a => implicitly[JsonWriter[A]].write(a),
          b => implicitly[JsonWriter[B]].write(b)
        )
      }
    }

    implicit def errorWriter: JsonWriter[CommandError] = {
      new JsonWriter[CommandError] {
        def write(e: CommandError) = JsObject("error" -> JsString(e.msg))
      }
    }
  }
}


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
  case class Response(id: RequestId, payload: Payload)
  object Response {
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
      if (msg.payload.nonEmpty) {
        log.info(s"Command message received: $msg")
        self ! Request(id = extractId(msg),
                       payload = ByteString(msg.payload.toArray))
      }
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
