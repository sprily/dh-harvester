package uk.co.sprily.dh
package harvester
package api

import scala.reflect.runtime.universe._

import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
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
import controllers.DeviceManager
import controllers.InstanceManager
import network.DeviceId
import network.IP4Address
import network.TCPGateway
import scheduling.Schedule

import modbus.ModbusDevice
import modbus.ModbusResponse

class AdhocRequestsEndpoint(
    root: Topic,
    client: ClientModule[Cont]#Client,
    instanceManager: ActorRef,
    override val timeout: FiniteDuration)
      extends ApiEndpoint(root, client) {

  import AdhocRequestsEndpoint.DTOs._
  import AdhocRequestsEndpoint.Errors
  import DeviceManager._

  type Command = ModbusAdhocRequest
  type Result = ModbusResponse

  override def commandReader = requestJson
  override def resultWriter  = responseJson

  override lazy val resultTypeTag = typeTag[Result]
  override val workerProps = Worker.props

  private[this] class Worker extends Actor with ActorLogging {
    def receive = {
      case (r: ModbusAdhocRequest) =>
        val req = ModbusProtocol.AdhocRequest(DeviceId(r.deviceId),
                                              r.from, r.to)
        instanceManager ! req

      case Protocol.UnknownDevice(id) =>
        context.parent ! response(Errors.UnknownDevice(id).left)

      case (r: ModbusResponse) => 
        context.parent ! response(r.right)
    }
  }

  private[this] object Worker {
    def props = Props(new Worker())
  }

}

object AdhocRequestsEndpoint {

  def props(root: Topic,
            client: ClientModule[Cont]#Client,
            instanceManager: ActorRef,
            timeout: FiniteDuration) = {
    Props(new AdhocRequestsEndpoint(root, client, instanceManager, timeout))
  }


  object Errors {
    import ApiEndpoint.Types._
    case class UnknownDevice(id: DeviceId) extends CommandError {
      def msg = s"Unknown device: $id"
    }
  }

  object DTOs extends DefaultJsonProtocol {

    implicit def requestJson  = jsonFormat3(ModbusAdhocRequest)
    implicit def responseJson = new RootJsonWriter[ModbusResponse] {
      override def write(r: ModbusResponse): JsObject = {
        JsObject(
          "deviceId" -> JsNumber(r.device.id.v),
          "payload"  -> JsString(r.measurement.values.toString))
      }
    }

    case class ModbusAdhocRequest(deviceId: Long, from: Int, to: Int)
  }

}
