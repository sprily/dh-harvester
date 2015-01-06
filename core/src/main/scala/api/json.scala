package uk.co.sprily.dh
package harvester
package api

import scala.util.Try
import scala.util.Success

import spray.json._

import modbus.ModbusDevice
import network.Device

// TODO: the spray JSON interface should really use Option/Try/...

object JsonFormats extends DefaultJsonProtocol {

  implicit val tcpGateway = jsonFormat2(TCPGatewayDTO)
  implicit private val modbusRequestDTO = jsonFormat3(ModbusRequestDTO)
  private val managedModbusDevice = jsonFormat4(ManagedModbusDevice)

  implicit val managedDeviceDTO: RootJsonFormat[ManagedDevice] = {
    new RootJsonFormat[ManagedDevice] {

      def read(js: JsValue): ManagedDevice = {
        js.asJsObject.fields.get("type").map {
          case JsString("modbus") => managedModbusDevice.read(js)
          case JsString(other)    => readError(s"Unable to match device type: $other")
          case _                  => readError("String 'type' field required")
        } getOrElse(readError("'type' field missing"))
      }

      def write(device: ManagedDevice): JsValue = {
        device match {
          case (d: ManagedModbusDevice) => managedModbusDevice.withTypeName("modbus").write(d)
          case _                        => writeError(s"Unable to write device $device")
        }
      }
    }
  }

  implicit val managedInstance = jsonFormat1(ManagedInstance)

  private def readError(msg: String) = {
    throw new DeserializationException(msg)
  }

  private def writeError(msg: String) = {
    throw new SerializationException(msg)
  }

  implicit class JsValueOps(json: JsValue) {
    def tryConvertTo[T: JsonReader]: Try[T] = {
      Try { jsonReader[T].read(json) }
    }
  }

  implicit class JsObjectOps(json: JsObject) {
    def update(k: String, v: JsValue) = json.copy(json.fields + (k -> v))
  }

  implicit class RootJsonFormatOps[T](fmt: RootJsonFormat[T]) {

    def withTypeName(name: String): RootJsonFormat[T] = new RootJsonFormat[T] {
      def read(json: JsValue): T = {
        json.asJsObject.fields.get("type").map {
          case JsString(n) if n == name => fmt.read(json)
          case JsString(other)          => readError(s"Unable to match device type: $other")
          case _                        => readError("String 'type' field required")
        } getOrElse(readError("'type' field missing"))
      }

      def write(t: T): JsValue = {
        fmt.write(t).asJsObject.update("type", JsString(name))
      }
    }

  }

}
