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

import network.Device

import protocols.codecs

/** A publisher of results from a given device.
  */
//class ResultsPublisher[D <: Device,M[+_]](
//    val topicRoot: Topic,
//    val device: D,
//    val bus: ResponseBus,
//    val client: ClientModule[M]#Client)(implicit val codec: Codec[D#Measurement])
//  extends Actor with ActorLogging {
//
//  // akka hooks.
//  // perform bus subscription upon *first* initialisation only
//  override def postRestart(reason: Throwable): Unit = ()
//  override def preStart(): Unit = {
//    bus.subscribe(self)
//  }
//
//  def receive = {
//    // cast is safe due to `bus.subscribe(self, device)` in preStart
//    case (r: Reading[_]) => publish(r.asInstanceOf[Reading[D]])
//  }
//
//  def publish(reading: Reading[D]) = {
//    val topic = topicFor(reading)
//    val payload: Array[Byte] = serialise(reading).toByteArray
//    log.info(s"Publishing to $topic: ${payload.length}")
//    client.publish(topic, payload)
//  }
//
//  private def topicFor(reading: Reading[D]) = Topic(s"${topicRoot.path}/${reading.device.id.v}/data/raw")
//
//  /** Note that no serialisation of Device data takes place, this is because
//    * it's already communicated in the topic path
//    */
//  private def serialise(r: Reading[D]) = {
//    (for {
//      tsBV <- codecs.localDateTime.encode(r.timestamp)
//      mBV  <- codec.encode(r.measurement)
//    } yield tsBV ++ mBV).getOrElse(???)
//  }
//
//}
//
//object ResultsPublisher {
//
//  def props[D <: Device, M[+_]]
//           (topicRoot: Topic,
//            d: D,
//            bus: ResponseBus,
//            client: ClientModule[M]#Client)
//           (implicit s: Codec[D#Measurement]): Props = {
//    Props(new ResultsPublisher[D,M](topicRoot, d, bus, client))
//  }
//
//}
