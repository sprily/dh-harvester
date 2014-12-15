package uk.co.sprily.dh
package harvester
package mqtt

import scala.language.higherKinds

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props

import spray.json._

import uk.co.sprily.mqtt.Cont
import uk.co.sprily.mqtt.ClientModule
import uk.co.sprily.mqtt.TopicPattern

import api.DeviceDTO
import api.JsonFormats
import api.Management
import api.ModbusDeviceDTO
import api.ModbusRequestDTO

import capture.DeviceManagerActor

import modbus.ModbusDevice

class Requests(
    reqTopic: TopicPattern,
    persReqTopic: TopicPattern,
    client: ClientModule[Cont]#Client)
  extends Actor with ActorLogging {

  import DeviceManagerActor.Protocol._

  // akka hooks.
  // perform broker subscription upon *first* initialisation only
  override def postRestart(reason: Throwable): Unit = ()
  override def preStart(): Unit = {

    client.data(reqTopic) { msg =>
      log.info(s"RCVD: $msg")
    }

    client.data(persReqTopic) { msg =>
      import JsonFormats._
      val json = new String(msg.payload.toArray, "UTF-8").parseJson.asJsObject

      val mgmtRequest = json.tryConvertTo[Management].get
      //val reqs = PersistentRequests(
      
      val reqs = for {
        pReq <- mgmtRequest.requests
        req <- pReq.dRequests
      } yield req

      val pReq = PersistentRequests(reqs)

      //val reqs = mgmtRequest.requests.head.requestsDTO.map {
      //  case (r: ModbusRequestDTO) => ModbusRequest(r.request(mgmtRequest.requests.head.device.asInstanceOf[ModbusDevice]))
      //}


      //val device = JsonFormats.convertToDevice(json).get
      //device match {
      //  case (d: ModbusDeviceDTO) => "some"
      //  case _                       => "other"
      //}
      log.info(s"RCVD: $reqs")
      context.actorSelection("../device-manager") ! pReq
    }

  }

  override def receive = {
    case _ => ()
  }

}

object Requests {
  def props(client: ClientModule[Cont]#Client) = Props(
    new Requests(reqPattern, pReqPattern, client)
  )

  def reqPattern = TopicPattern("test-org/todo")
  def pReqPattern = TopicPattern("test-org/todo/too")
}
