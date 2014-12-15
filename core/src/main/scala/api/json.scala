package uk.co.sprily.dh
package harvester
package api

import scala.reflect.runtime.universe._

import spray.json._

import modbus.ModbusDevice
import network.Device

// TODO: the spray JSON interface should really use Option/Try/...

object JsonFormats extends DefaultJsonProtocol {

  def convertToDevice(json: JsObject): Option[DeviceDTO[_]] = {
    json.fields.get("type").flatMap {
      case JsString("modbus") => json.tryConvertTo[ModbusDeviceDTO]
      case _                  => None
    }
  }

  def convertToPReq(json: JsObject): Option[PersistentRequestDTO[_]] = {
    val deviceDTO = json.fields.get("device").flatMap(js => convertToDevice(js.asJsObject))
    deviceDTO.flatMap {
      case d@ModbusDeviceDTO(_,_,_) => 
        json.fields.get("requests")
          .flatMap(_.tryConvertTo[Seq[ModbusRequestDTO]])
          .map(PersistentRequestDTO(d, _))
        
    }
  }

  implicit def PRequest: RootJsonFormat[PersistentRequestDTO[_]] = {
    new RootJsonFormat[PersistentRequestDTO[_]] {
      def read(json: JsValue): PersistentRequestDTO[_] = {
        val deviceDTO = json.asJsObject.fields.get("device").flatMap(js => convertToDevice(js.asJsObject))
        deviceDTO.flatMap {
          case d@ModbusDeviceDTO(_,_,_) =>
            json.asJsObject.fields.get("requests")
              .flatMap(_.tryConvertTo[Seq[ModbusRequestDTO]])
              .map(PersistentRequestDTO(d, _))
        }.get   // throw DeserializationError
      }

      def write(r: PersistentRequestDTO[_]): JsValue = {
        r.deviceDTO match {
          case d@ModbusDeviceDTO(_,_,_) => d.toJson
        }
      }
    }
  }

  implicit val mgmt = jsonFormat1(Management)
  implicit val tcpGateway = jsonFormat2(TCPGatewayDTO)
  implicit val modbusDevice = jsonFormat3(ModbusDeviceDTO).withTypeName
  implicit val modbusRequest = jsonFormat3(ModbusRequestDTO)

  implicit class JsValueOps(json: JsValue) {
    def tryConvertTo[T: JsonReader]: Option[T] = {
      try { Some(jsonReader[T].read(json)) }
      catch { case (e: SerializationException) => None }
    }
  }

  implicit class JsObjectOps(json: JsObject) {
    def update(k: String, v: JsValue) = json.copy(json.fields + (k -> v))
  }

  implicit class RootJsonFormatOps[T : NamedType](fmt: RootJsonFormat[T]) {

    private val name = implicitly[NamedType[T]].typeName

    def withTypeName: RootJsonFormat[T] = new RootJsonFormat[T] {
      def read(json: JsValue): T = {
        json.asJsObject.fields("type") match {
          case JsString(name) => fmt.read(json)
        }
      }
      def write(t: T): JsValue = {
        fmt.write(t).asJsObject.update("type", JsString(name))
      }
    }

  }

  trait NamedType[T] {
    def typeName: String
  }

  object NamedType {
    def apply[T](name: String): NamedType[T] = new NamedType[T] {
      def typeName = name
    }

    implicit def modbusDevice: NamedType[ModbusDeviceDTO] = NamedType[ModbusDeviceDTO]("modbus")

  }

}
