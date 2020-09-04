package attempt
import net.liftweb.json
import net.liftweb.json.{JNothing, JNull, _}
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonAST.{JObject, JValue}
import org.json4s.JsonAST.JDouble

import scala.util.{Failure, Success, Try}
object Util {
  case class RawRecords(records:List[RawRecord])
  case class RawRecord(metadata:Map[String,Any], payload:Map[String,Any])

  def getJsonObj(jsonString:String):Any= {
    extractFrom(jsonString) match {
      case Success(jsonParsed) =>
        jsonParsed
      case Failure(exc) =>
        throw new IllegalArgumentException(exc)
    }
  }


   private def extractFrom(s: String):Try[Any] = {
    Try {
      val jVal = parse(s)
      println(jVal.values.getClass)
      jVal.values
      }
    }

}
