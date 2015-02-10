package uk.co.sprily.dh
package harvester
package api

import scala.util.Try
import scala.util.Success

import spray.json._

import modbus.ModbusDevice

// TODO: the spray JSON interface should really use Option/Try/...

trait JsonUtils {

  protected def readError(msg: String) = {
    throw new DeserializationException(msg)
  }

  protected def writeError(msg: String) = {
    throw new SerializationException(msg)
  }

  implicit class JsValueOps(json: JsValue) {
    def tryConvertTo[T: JsonReader]: Try[T] = {
      Try { jsonReader[T].read(json) }
    }
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

  implicit class JsObjectOps(json: JsObject) {
    def update(k: String, v: JsValue) = json.copy(json.fields + (k -> v))
  }

}
