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
  *
  * There's a large song-and-dance over the dependent type, `module.Client`,
  * because there's no way to have classes with dependently typed constructor
  * arguments.  The workaround is to define what would be the constructor
  * arguments as abstract fields in a trait, and then use the apply method
  * on the companion object to construct new instantiations of
  * ResultsPublisher.  It's not pretty, and really it's because of an
  * oversight in the scala-mqtt API, which may well be fixed soon.
  */
trait ResultsPublisher[D <: Device, M[+_], CM <: ClientModule[M]]
    extends Actor with ActorLogging {

  // Abstract fields
  val topicRoot: Topic
  val device: D
  val bus: DeviceBus
  val module: CM
  val client: module.Client
  implicit val codec: Codec[D#Measurement]

  import module._

  // akka hooks.
  // perform bus subscription upon *first* initialisation only
  override def postRestart(reason: Throwable): Unit = ()
  override def preStart(): Unit = {
    bus.subscribe(self, device)
  }

  def receive = {
    // cast is safe due to `bus.subscribe(self, device)` in preStart
    case (r: Reading[_]) => publish(r.asInstanceOf[Reading[D]])
  }

  def publish(reading: Reading[D]) = {
    val topic = topicFor(reading)
    val payload: Array[Byte] = serialise(reading).toByteArray
    log.info(s"Publishing to $topic: ${payload.length}")
    client.publish(topic, payload)
  }

  private def topicFor(reading: Reading[D]) = Topic(s"${topicRoot.path}/${reading.device.id.v}/data/raw")

  /** Note that no serialisation of Device data takes place, this is because
    * it's already communicated in the topic path
    */
  private def serialise(r: Reading[D]) = {
    (for {
      tsBV <- codecs.localDateTime.encode(r.timestamp)
      mBV  <- codec.encode(r.measurement)
    } yield tsBV ++ mBV).getOrElse(???)
  }

}

object ResultsPublisher {

  def apply[D <: Device, M[+_], CM <: ClientModule[M]]
           (_topicRoot: Topic,
            _d: D,
            _bus: DeviceBus,
            _module: CM)
           (_client: _module.Client)
           (implicit c: Codec[D#Measurement]): ResultsPublisher[D,M,CM] = {
    new ResultsPublisher[D,M,CM] {
      lazy val topicRoot = _topicRoot
      lazy val device = _d
      lazy val bus = _bus
      lazy val module = _module
      lazy val client = _client.asInstanceOf[module.Client]
      lazy val codec = c
    }
  }
  def props[D <: Device, M[+_], CM <: ClientModule[M]]
           (topicRoot: Topic,
            d: D,
            bus: DeviceBus,
            module: CM)
           (client: module.Client)
           (implicit s: Codec[D#Measurement]): Props = {
    Props(ResultsPublisher[D,M,CM](topicRoot, d, bus, module)(client))
  }
}
