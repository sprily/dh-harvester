package uk.co.sprily.dh
package harvester
package api

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props

import scalaz._
import Scalaz._

import spray.json._

import uk.co.sprily.mqtt.Cont
import uk.co.sprily.mqtt.ClientModule
import uk.co.sprily.mqtt.TopicPattern
import uk.co.sprily.mqtt.Topic
import uk.co.sprily.mqtt.MqttMessage

import actors.ApiEndpoint

class InstanceApi(
    root: Topic,
    client: ClientModule[Cont]#Client)
      extends ApiEndpoint(root, client) {

  import ApiEndpoint.Types._
  import InstanceApi._

  type Command = InstanceConfiguration
  type Result = String
  override def commandReader = configJson
  override def resultWriter  = implicitly[JsonWriter[String]]

  //override def childProps(id: RequestId, c: InstanceConfiguration) = {
  //  Props(new Actor() {
  //    def receive = {
  //      case (config: InstanceConfiguration) =>
  //        context.parent ! Response(id, TimedOutError.left)
  //        //context.parent ! Response(id, "foobar".right)
  //    }
  //  })
  //}

}

object InstanceApi extends DefaultJsonProtocol {

  def props(root: Topic, client: ClientModule[Cont]#Client) = {
    Props(new InstanceApi(root, client))
  }

  implicit def requestJson = jsonFormat2(ManagedModbusRequest)
  implicit def deviceJson  = jsonFormat5(ManagedModbusDevice)
  implicit def configJson  = jsonFormat1(InstanceConfiguration)

  case class InstanceConfiguration(devices: List[ManagedModbusDevice])

  case class ManagedModbusDevice(
    id: Int,
    host: String,
    port: Int,
    deviceOffset: Int,
    requests: List[ManagedModbusRequest]) {

  }

  case class ManagedModbusRequest(from: Int, to: Int) {

  }

}
