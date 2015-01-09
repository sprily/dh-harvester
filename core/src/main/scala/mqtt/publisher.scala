package uk.co.sprily.dh
package harvester
package mqtt

import scala.language.higherKinds

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props

import org.joda.time.LocalDateTime

import scodec.Codec

import uk.co.sprily.mqtt.ClientModule
import uk.co.sprily.mqtt.Topic

import capture.ResponseLike
import network.DeviceLike
import protocols.codecs
import modbus.ModbusResponse

class ResultsPublisher[M[+_]](
    val topicRoot: Topic,
    val bus: ResponseBus,
    val mqttClient: ClientModule[M]#Client) extends Actor with ActorLogging {

  // akka hooks.
  // perform bus subscription upon *first* initialisation only
  override def postRestart(reason: Throwable): Unit = ()
  override def preStart(): Unit = {
    bus.subscribe(self)
  }
  
  def receive = {
    case (r: ModbusResponse) => publish(r)
  }

  def publish(r: ModbusResponse)(implicit codec: Codec[ModbusResponse#Measurement]) = {
    val topic = topicFor(r)
    val payload: Array[Byte] = serialise(r).toByteArray
    log.info(s"Publishing to $topic: ${payload.length}")
    mqttClient.publish(topic, payload)
  }

  private final def topicFor(r: ResponseLike) = {
    Topic(s"${topicRoot.path}/${r.device.id.v}/data/raw")
  }

  private final def serialise(r: ResponseLike)(implicit codec: Codec[r.Measurement]) = {
    (for {
      tsBV <- codecs.localDateTime.encode(r.timestamp)
      mBV  <- codec.encode(r.measurement)
    } yield tsBV ++ mBV).getOrElse(???)
  }
}
