package uk.co.sprily.dh
package harvester
package mqtt

import scala.language.higherKinds

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props

import uk.co.sprily.mqtt.ClientModule
import uk.co.sprily.mqtt.Topic

import network.Device

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
  val topicRoot: String
  val device: D
  val bus: DeviceBus
  val module: CM
  val client: module.Client
  implicit val serialiser: Serialiser[D#Measurement]

  import module._

  // akka hooks.
  // perform bus subscription upon *first* initialisation only
  override def postRestart(reason: Throwable): Unit = ()
  override def preStart(): Unit = {
    bus.subscribe(self, device)
  }

  def receive = {
    case (r: Reading[_]) => publish(r.asInstanceOf[Reading[D]])
  }

  def publish(reading: Reading[D]) = {
    val topic = topicFor(reading)
    val payload = serialiser.toBytes(reading.measurement).toArray
    log.debug(s"Publishing to $topic: $payload")
    client.publish(topic, payload)
  }

  private def topicFor(reading: Reading[D]) = Topic(s"${topicRoot}/${reading.device.id}")

}

object ResultsPublisher {

  def apply[D <: Device, M[+_], CM <: ClientModule[M]]
           (_topicRoot: String,
            _d: D,
            _bus: DeviceBus,
            _module: CM)
           (_client: _module.Client)
           (implicit s: Serialiser[D#Measurement]): ResultsPublisher[D,M,CM] = {
    new ResultsPublisher[D,M,CM] {
      lazy val topicRoot = _topicRoot
      lazy val device = _d
      lazy val bus = _bus
      lazy val module = _module
      lazy val client = _client.asInstanceOf[module.Client]
      lazy val serialiser = s
    }
  }
  def props[D <: Device, M[+_], CM <: ClientModule[M]]
           (topicRoot: String,
            d: D,
            bus: DeviceBus,
            module: CM)
           (client: module.Client)
           (implicit s: Serialiser[D#Measurement]): Props = {
    Props(ResultsPublisher[D,M,CM](topicRoot, d, bus, module)(client))
  }
}
