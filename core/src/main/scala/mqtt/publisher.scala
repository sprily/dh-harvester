package uk.co.sprily.dh
package harvester
package mqtt

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props

import uk.co.sprily.mqtt.AsyncSimpleClient

import network.Device

class ResultsPublisher[D <: Device](
    topicRoot: String,
    device: D,
    bus: DeviceBus,
    mqttClient: AsyncSimpleClient.Client)
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
  def props[D <: Device](topicRoot: String,
                         d: D,
                         bus: DeviceBus,
                         client: AsyncSimpleClient.Client)
                        (implicit s: Serialiser[D#Measurement]): Props = {
    Props(new ResultsPublisher(topicRoot, d, bus, client))
  }
}
