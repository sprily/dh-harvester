package uk.co.sprily.dh
package harvester
package actors

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions
import org.specs2.scalaz.ValidationMatchers

import scalaz._
import Scalaz._

import spray.json._

import uk.co.sprily.mqtt.Cont
import uk.co.sprily.mqtt.ClientModule
import uk.co.sprily.mqtt.TopicPattern
import uk.co.sprily.mqtt.Topic
import uk.co.sprily.mqtt.MqttMessage

import api.JsonUtils

class MqttCommandActorSpec extends SpecificationLike
                              with JsonUtils
                              with NoTimeConversions {

  import MqttCommandActor.Types._

  "A MqttCommandActor" should {
 
    "write a successful response back to the broker" in new Context {
      val underTest = TestActorRef(new TestCommandActor())
      fakeModule.onDataReceived { (data: (Topic, List[Byte])) => testActor ! data }
      fakeModule.fakeIssueCommand("req-id-1", """{"name": "command-1"}""")

      val expectedResult = Res(value=9)
      val response = expectMsgType[(Topic, List[Byte])]
      val result = (new String(response._2.toArray, "UTF-8")).parseJson.asJsObject.convertTo[Res]

      (response._1 must === (Topic("root/commands/req-id-1/response"))) &&
      (result must === (expectedResult))
      checkRequestTopicCleared("req-id-1")
    }

    "respond to json de-serialisation errors by sending error response to broker" in new Context {
      val underTest = TestActorRef(new TestCommandActor())
      fakeModule.onDataReceived { (data: (Topic, List[Byte])) => testActor ! data }
      fakeModule.fakeIssueCommand("req-id-2", """{}""")

      val response = expectMsgType[(Topic, List[Byte])]
      val error = new String(response._2.toArray, "UTF-8").parseJson.asJsObject
      (response._1 must === (Topic("root/commands/req-id-2/response"))) &&
      (error.fields.get("error") must not be (None))
      checkRequestTopicCleared("req-id-2")
    }

    "respond to error result from child by sending response to broker" in new Context {
      val underTest = TestActorRef(new TestCommandActor())
      fakeModule.onDataReceived { (data: (Topic, List[Byte])) => testActor ! data }
      fakeModule.fakeIssueCommand("req-id-2", """{"name": "return-error"}""")

      val response = expectMsgType[(Topic, List[Byte])]
      val error = new String(response._2.toArray, "UTF-8").parseJson.asJsObject
      (response._1 must === (Topic("root/commands/req-id-2/response"))) &&
      (error.fields.get("error") must not be (None))
      checkRequestTopicCleared("req-id-2")
    }

    "respond to exception in child by sending response to broker" in new Context {
      val underTest = TestActorRef(new TestCommandActor())
      fakeModule.onDataReceived { (data: (Topic, List[Byte])) => testActor ! data }
      fakeModule.fakeIssueCommand("req-id-2", """{"name": "throw-error"}""")

      val response = expectMsgType[(Topic, List[Byte])]
      val error = new String(response._2.toArray, "UTF-8").parseJson.asJsObject
      (response._1 must === (Topic("root/commands/req-id-2/response"))) &&
      (error.fields.get("error") must not be (None))
      checkRequestTopicCleared("req-id-2")
    }

  }

  class Context extends AkkaSpecs2Support with ImplicitSender with DefaultJsonProtocol {

    final def checkRequestTopicCleared(id: String) = {
      val clear = expectMsgType[(Topic, List[Byte])]  // clearing the topic
      (clear._1 must === (Topic(s"root/commands/$id/request"))) &&
      (clear._2 must === (Nil))
    }

    import uk.co.sprily.mqtt._

    def root = TopicPattern("root")
    lazy val fakeModule = new FakeMqttModule()
    lazy val fakeClient = Await.result(fakeModule.connect(MqttOptions.cleanSession()), 1.second)

    case class Cmd(name: String)
    case class Res(value: Int)
    implicit def CmdJson: JsonFormat[Cmd] = jsonFormat1(Cmd)
    implicit def ResJson: JsonFormat[Res] = jsonFormat1(Res)

    class TestCommandActor extends MqttCommandActor(root, fakeClient) {
      type Command = Cmd
      type Result = Res
      override def commandJson = jsonFormat1(Cmd)
      override def resultJson = jsonFormat1(Res)
      override def childProps(id: RequestId, c: Command) = Props(new Actor() {

        def receive = {
          case Cmd("return-error") =>
            context.parent ! Response(id, TimedOutError.left)
          case Cmd("throw-error") =>
            throw new Exception("Something went wrong")
          case Cmd(other) =>
            context.parent ! Response(id, Res(other.length).right)
        }

      })
    }

    class FakeMqttModule extends ClientModule[Cont] {


      type Data = (Topic, List[Byte])
      var dataRcvdListeners = List.empty[Data => Unit]
      var dataPublishedListeners = List.empty[MqttMessage => Unit]

      def onDataReceived(cb: Data => Unit) = {
        dataRcvdListeners = cb :: dataRcvdListeners
      }

      def fakeIssueCommand(requestId: String, payload: String) = {
        val topic = Topic(root.path + s"/commands/$requestId/request")
        val m = MqttMessage(topic, payload.getBytes.toList)
        dataPublishedListeners.foreach(_(m))
      }

      case class Client() extends ClientLike
      override def connect(options: MqttOptions) = Future { Client() }
      override def disconnect(client: Client) = Future { () }
      override def status(client: Client) = ???
      override def data(client: Client) = ???
      override def publish(client: Client,
                           topic: Topic,
                           payload: Array[Byte],
                           qos: QoS = AtLeastOnce,
                           retain: Boolean = false) = {
        dataRcvdListeners.foreach { cb => cb((topic, payload.toList)) }
        Future { }
      }

      override def data(client: Client, topics: Seq[TopicPattern]): Cont[MqttMessage] = { f =>
        dataPublishedListeners = f :: dataPublishedListeners
      }
    }

  }

}
