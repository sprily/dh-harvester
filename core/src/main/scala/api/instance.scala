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
import controllers.InstanceManager
import network.DeviceId
import network.IP4Address
import network.TCPGateway
import capture.RequestLike

import modbus.ModbusDevice
import modbus.ModbusDeviceAddress
import modbus.ModbusRequest
import modbus.ModbusRegisterRange

class InstanceApi(
    root: Topic,
    client: ClientModule[Cont]#Client,
    instanceManager: ActorRef,
    override val timeout: FiniteDuration)
      extends ApiEndpoint(root, client) {

  import InstanceApi._
  import InstanceApi.DTOs._
  import InstanceManager.Protocol._

  type Command = InstanceConfiguration
  type Result = String
  override def commandReader = configJson
  override def resultWriter  = implicitly[JsonWriter[String]]
  override lazy val resultTypeTag = typeTag[Result]
  override val workerProps = Worker.props

  private[this] class Worker extends Actor with ActorLogging {
    def receive = {
      case config@InstanceConfiguration(devices) =>
        config.requests match {
          case -\/(errs) => context.parent ! response(errs.left)
          case \/-(reqs) => instanceManager ! InstanceConfig(reqs)
        }
      case Acked =>
        context.parent ! response("acked".right)
    }
  }

  private[this] object Worker {
    def props = Props(new Worker())
  }

}

object InstanceApi extends DefaultJsonProtocol {

  import ApiEndpoint.Types._

  def props(root: Topic,
            client: ClientModule[Cont]#Client,
            instanceManager: ActorRef,
            timeout: FiniteDuration) = {
    Props(new InstanceApi(root, client, instanceManager, timeout))
  }

  object DTOs {

    implicit def requestJson = jsonFormat2(ManagedModbusRequest)
    implicit def deviceJson  = jsonFormat5(ManagedModbusDevice)
    implicit def configJson  = jsonFormat1(InstanceConfiguration)

    case class InstanceConfiguration(devices: List[ManagedModbusDevice]) {
      def requests: \/[RequestError,Seq[RequestLike]] = {
        devices.map(_.validRequests).sequenceU.map(_.flatten).disjunction.leftMap { errs =>
          new RequestError(errs.toList.mkString("\n"))
        }
      }
    }

    case class ManagedModbusDevice(
        id: Int,
        host: String,
        port: Int,
        slaveAddress: Byte,
        requests: List[ManagedModbusRequest]) {

      def device: ValidationNel[String,ModbusDevice] = {
        (DeviceId(id).successNel[String] |@| validDeviceAddress)(ModbusDevice)
      }

      def validRequests: ValidationNel[String, List[RequestLike]] = {
        (device.disjunction >>= { d: ModbusDevice =>
          requests.zipWithIndex.map { case (mgdReq, idx) =>
            mgdReq.request(idx, d)
          }.sequenceU.disjunction
        }).validation
      }

      private def validDeviceAddress = {
        (validSlaveAddress |@| validGateway)(ModbusDeviceAddress)
      }

      private def validGateway = {
        (IP4Address.validated(host) |@| validPort)(TCPGateway)
      }

      private def validPort = port match {
        case port if port < 1 => "Negative port".failureNel
        case port             => port.successNel
      }

      private def validSlaveAddress = slaveAddress match {
        case slave if slave < 0   => "Negative slave address".failureNel
        case slave if slave > 255 => "Slave address > 255".failureNel
        case slave                => slave.successNel
      }

    }

    case class ManagedModbusRequest(from: Int, to: Int) {
      def request(idx: Int, device: ModbusDevice): ValidationNel[String,RequestLike] = {
        ModbusRegisterRange.validated(from, to).map { range =>
          ModbusRequest(
            id=idx,
            device=device,
            selection=range)
        }
      }
    }

  }

}
