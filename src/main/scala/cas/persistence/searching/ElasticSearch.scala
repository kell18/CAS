package cas.persistence.searching

import spray.client.pipelining._
import spray.http.{HttpEntity, HttpRequest, Uri}
import akka.actor.ActorSystem
import spray.client.pipelining._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.http.MediaTypes.`application/json`
import spray.httpx.unmarshalling.Unmarshaller
import spray.json._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.httpx.unmarshalling.Deserialized
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import cas.utils.StdImplicits.RightBiasedEither
import cas.utils.UtilAliases.ErrorMsg


object ElasticProtocol extends DefaultJsonProtocol {
  val field = "doc"

  case class ShortResponse(isCreated: Boolean)
  implicit val shortResponseFormat = jsonFormat(ShortResponse, "created")

  case class DelResponse(found: Boolean)
  implicit val delResponseFormat = jsonFormat1(DelResponse)


  case class SearchResponse(maxScore: Option[Double], total: Int) {
  }
  implicit val searchResponseFormat = jsonFormat(SearchResponse, "max_score" ,"total")
  implicit def searchRespUnmarsh = Unmarshaller[SearchResponse](`application/json`) {
    case HttpEntity.NonEmpty(contentType, data) =>
      data.asString.parseJson.asJsObject.fields("hits").convertTo[SearchResponse]
    case HttpEntity.Empty => deserializationError("Empty entity. SearchResponse expected")
  }

  case class GetResponse(value: Option[String])
  implicit val getResponseFormat = jsonFormat(GetResponse, field)
  implicit def getRespUnmarsh = Unmarshaller[GetResponse] (`application/json`) {
    case HttpEntity.NonEmpty(contentType, data) =>
      data.asString.parseJson.asJsObject.fields("_source").convertTo[GetResponse]
    case HttpEntity.Empty => deserializationError("Empty entity. SearchResponse expected")
  }

  case class EsError(kind: String, reason: String) { def getMessage: ErrorMsg = s"ES Error: $kind : $reason" }
  implicit val esErrorFormat = jsonFormat(EsError, "type", "reason")
  implicit def esErrUnmarsh = Unmarshaller[EsError] (`application/json`) {
    case HttpEntity.NonEmpty(contentType, data) =>
      data.asString.parseJson.asJsObject.fields("error").convertTo[EsError]
    case HttpEntity.Empty => deserializationError("Empty entity. EsError expected")
  }

  case class EsFallible[R](errorOrResp: Either[ErrorMsg, R])
  implicit def esFallibleFormat[R: JsonFormat] = jsonFormat1(EsFallible.apply[R])
  implicit def esFallibleUnmarsh[R: JsonFormat](implicit unmarsh: Unmarshaller[R]) =
    Unmarshaller[EsFallible[R]] (`application/json`) {
      case HttpEntity.NonEmpty(contentType, data) => unmarsh(HttpEntity(contentType, data)) match {
          case Right(r) => EsFallible[R](Right(r))
          case Left(NonFatal(ex)) => esErrUnmarsh(HttpEntity(contentType, data)) match {
            case Right(err) => EsFallible[R](Left(err.getMessage))
            case Left(e) => deserializationError(s"Fail to unmarshal ES error: `$e")
          }
        }
      case HttpEntity.Empty => deserializationError("Empty entity. EsFallible expected")
    }

  case class EsAck(acknowledged: Boolean)
  implicit val esAckFormat = jsonFormat1(EsAck)
}

class ElasticSearch(val host: String, index: String = "cas-ind", mtype: String = "docs", ttl: Duration = Duration("48 hours"))
                   (implicit val system: ActorSystem) {
  import ElasticProtocol._
  import spray.httpx.SprayJsonSupport._
  import spray.json.DefaultJsonProtocol._

  val analyzer = "russian"
  val address = host + "/" + index + "/" + mtype

  def queryEntity(entity: => String) = {
    val url = address + "/_search"
    val data = s"""{ \"query\": { \"match\": { \"$field\": { \"analyzer\": \"$analyzer\", \"query\": \"$entity\" } } } }"""
    val pipeline = sendReceive ~> unmarshal[EsFallible[SearchResponse]]
    pipeline(Get(Uri(url), data)) map { _.errorOrResp }
  }

  def getEntity(id: String) = {
    val url = address + "/" + id
    val pipeline = sendReceive ~> unmarshal[EsFallible[GetResponse]]
    pipeline(Get(Uri(url))) map { _.errorOrResp map {_.value} }
  }

  def pushIndexedEntity(id: String, entity: => String) = {
    val url = address + "/" + id
    val data = s"""{ "$field": "$entity" }"""
    val pipeline = sendReceive ~> unmarshal[EsFallible[ShortResponse]]
    pipeline(Put(Uri(url), data)) map { _.errorOrResp map {_.isCreated} }
  }

  def delIndexedEntity(id: String) = {
    val url = address + "/" + id
    val pipeline = sendReceive ~> unmarshal[EsFallible[DelResponse]]
    pipeline(Delete(Uri(url))) map { _.errorOrResp map {_.found} }
  }

  def initStorage = {
    import spray.json.DefaultJsonProtocol._
    val pipeline = sendReceive ~> unmarshal[JsObject]
    pipeline(Get(Uri(host + "/_aliases"))) flatMap { indexes =>
      if (!indexes.fields.contains(index))
        createIndex
      else {
        system.log.warning(s"[ElasticSearch] Index $index already exists, will use it.")
        Future { Right(true) }
      }
    }
  }

  def disposeStorage = {
    val url = host + "/" + index
    val pipeline = sendReceive ~> unmarshal[EsFallible[EsAck]]
    pipeline(Delete(Uri(url))) map { _.errorOrResp map {_.acknowledged} }
  }

  private def createIndex = {
    val pipeline = sendReceive ~> unmarshal[EsFallible[EsAck]]
    pipeline(Put(Uri(host + "/" + index), indexSchema)) map {_.errorOrResp map {_.acknowledged} }
  }

  val indexSchema =
    s"""{
       "settings": {
         "analysis": {
           "analyzer": "$analyzer"
         }
       },
       "mappings": {
         "_default_": {
           "_ttl": {
             "enabled": true,
             "default": "${ttl.toSeconds}s"
           }
         },
         "$mtype": {
           "properties": {
             "$field": {
               "type":     "string",
               "analyzer": "$analyzer"
             }
           }
         }
       }
     }"""
}
