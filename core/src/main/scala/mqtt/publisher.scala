package uk.co.sprily.dh
package harvester
package mqtt

import scala.language.higherKinds

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props

import uk.co.sprily.mqtt.ClientModule

import network.Device

class ResultsPublisher[D <: Device, M[+_]](
    topicRoot: String,
    device: D,
    bus: DeviceBus,
    mqttClient: ClientModule[M]#Client)
    (implicit serialiser: Serialiser[D#Measurement]) extends Actor with ActorLogging {

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
    log.warning(s"Publishing ${serialiser.toBytes(reading.measurement)}")
  }

}

object ResultsPublisher {
  def props[D <: Device, M[+_]]
           (topicRoot: String,
            d: D,
            bus: DeviceBus,
            client: ClientModule[M]#Client)
           (implicit s: Serialiser[D#Measurement]): Props = {
    Props(new ResultsPublisher[D,M](topicRoot, d, bus, client))
  }
}
