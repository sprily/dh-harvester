package uk.co.sprily.dh
package harvester
package mqtt

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.testkit.TestActorRef

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
import modbus.ModbusDevice
import modbus.ModbusDeviceAddress
import modbus.ModbusMeasurement
import modbus.ModbusRegisterRange
import modbus.Word16

import network.Device
import network.DeviceId
import network.IP4Address
import network.TCPGateway

class ResultsPublisherSpec extends SpecificationLike
                              with NoTimeConversions {

  "A ResultsPublisher" should {

    "Publish to the correct MQTT topic" in new PublisherContext {
      val underTest = publisher(topicRoot = "root/sub-root")
      val measurement = ModbusMeasurement(
        range = ModbusRegisterRange(50210, 50220),
        words = List(Word16(0xff), Word16(0xf0)))

      underTest ! reading(measurement)
      expectMsgType[(Topic,Array[Byte])]._1 must === (Topic("root/sub-root/10"))
    }

  }

}

class PublisherContext extends AkkaSpecs2Support {

  type Id[+T] = T
  type Seq[+A] = scala.collection.immutable.Seq[A]

  def publisher(topicRoot: String) = {
    TestActorRef(
      ResultsPublisher[ModbusDevice,Id,ClientModule[Id]](
        topicRoot,
        modbusDevice,
        fakeBus,
        fakeModule)(fakeClient)
    )
  }

  lazy val modbusDevice = ModbusDevice(
    id = DeviceId(10L),
    address = ModbusDeviceAddress(
      deviceNumber = 0x12,
      gateway = TCPGateway(
        address = IP4Address.localhost,
        port = 5432)))

  lazy val fakeBus = new DeviceBus {
    def publish[D <: Device](r: Reading[D]) = noOp
    def subscribe[D <: Device](s: ActorRef, d: D) = true
    def unsubscribe[D <: Device](s: ActorRef, d: D) = true
    def unsubscribe(s: ActorRef) = noOp

    def noOp() = ()
  }

  lazy val fakeModule = new ClientModule[Id] {
    import scala.concurrent.ExecutionContext.Implicits.global
    case class Client()
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

  def reading(m: ModbusMeasurement) = Reading[ModbusDevice](
    timestamp = LocalDateTime.now(),
    device = modbusDevice,
    measurement = m)
}