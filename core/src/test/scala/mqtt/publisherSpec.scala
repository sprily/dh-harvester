package uk.co.sprily.dh
package harvester
package mqtt

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.testkit.TestActorRef
import akka.util.ByteString

import org.joda.time.LocalDateTime

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions

import uk.co.sprily.mqtt.AtLeastOnce
import uk.co.sprily.mqtt.ClientModule
import uk.co.sprily.mqtt.MqttMessage
import uk.co.sprily.mqtt.MqttOptions
import uk.co.sprily.mqtt.QoS
import uk.co.sprily.mqtt.Topic
import uk.co.sprily.mqtt.TopicPattern

import capture.ResponseLike

import modbus.ModbusDevice
import modbus.ModbusDeviceAddress
import modbus.ModbusMeasurement
import modbus.ModbusRegisterRange
import modbus.ModbusResponse

import network.DeviceLike
import network.DeviceId
import network.IP4Address
import network.TCPGateway

class ResultsPublisherSpec extends SpecificationLike
                              with NoTimeConversions {

  "A ResultsPublisher" should {

    "Publish to the correct MQTT topic" in new TestContext {
      val underTest = publisher(topicRoot = Topic("root/sub-root"))
      val measurement = ModbusMeasurement(
        range = ModbusRegisterRange(50210, 50220),
        values = ByteString.fromString("payload-data"))

      underTest ! response(measurement)
      expectMsgType[(Topic,Array[Byte])]._1 must === (Topic("root/sub-root/10/data/raw"))
    }

  }

  class TestContext extends AkkaSpecs2Support {

    type Id[+T] = T
    type Seq[+A] = scala.collection.immutable.Seq[A]

    def publisher(topicRoot: Topic) = {
      TestActorRef(
        new ResultsPublisher[Id](
          topicRoot,
          fakeBus,
          fakeClient)
      )
    }

    lazy val modbusDevice = ModbusDevice(
      id = DeviceId(10L),
      address = ModbusDeviceAddress(
        deviceNumber = 0x12,
        gateway = TCPGateway(
          address = IP4Address.localhost,
          port = 5432)))

    lazy val fakeBus = new ResponseBus {
      def publish(r: ResponseLike) = noOp
      def subscribe(s: ActorRef) = true
      def unsubscribe(s: ActorRef) = true

      def noOp() = ()
    }

    lazy val fakeModule = new ClientModule[Id] {
      import scala.concurrent.ExecutionContext.Implicits.global
      case class Client() extends ClientLike
      override def connect(options: MqttOptions) = Future { Client() }
      override def disconnect(client: Client) = Future { () }
      override def status(client: Client) = ???
      override def data(client: Client) = ???
      override def data(client: Client, topics: Seq[TopicPattern]): Id[MqttMessage] = ???
      override def publish(client: Client,
                  topic: Topic,
                  payload: Array[Byte],
                  qos: QoS = AtLeastOnce,
                  retain: Boolean = false) = Future {
        val msg = (topic, payload)
        testActor ! msg
      }
    }

    lazy val fakeClient = Await.result(
      fakeModule.connect(MqttOptions.cleanSession()),
      1.second)

    def response(m: ModbusMeasurement) = ModbusResponse(
      timestamp = LocalDateTime.now(),
      device = modbusDevice,
      measurement = m)
  }
}

