package cas.web.dealers

import cas.subject.components.{Description, Likability, CreationDate, Component}

import scala.concurrent.Future
import spray.json.{JsonFormat, DefaultJsonProtocol}
import spray.json._
import spray.util._
import spray.http._
import spray.httpx.unmarshalling._
import spray.http.HttpResponse
import spray.httpx.unmarshalling.{Unmarshaller, FromResponseUnmarshaller, MalformedContent}
import spray.client.pipelining._


case class VkResponse[T](count: Int, items: List[T])

case class VkPost(id: Int /*TODO*/)
case class VkComment(id: Int, from_id: Long, date: Long, text: String, likes: Int)

object VkApiJsonProtocol extends DefaultJsonProtocol {
  implicit val vkPostJsonFormat = jsonFormat1(VkPost)
  // implicit val vkCommentJsonFormat = jsonFormat(VkComment, "id", "from_id", "date", "text", "likes.count")

  implicit object VkCommentFormat extends JsonFormat[VkComment] {
    def write(obj: VkComment): JsValue = {
      // TODO: Implement
      ???
    }

    def read(json: JsValue): VkComment = json match {
      case JsObject(M"id" -> JsNumber(id), "from_id" -> JsNumber(fromId),
                    "date" -> JsNumber(date), "text" -> JsString(txt), "likes" -> JsObject(likes)) =>
        VkComment(id, fromId, date.toLong, txt, likes("count").convertTo[Int])
      case x => {
        println("Unknown match: " + x.toString)
        ???
      }
    }
  }

  implicit def vkResponseJsonFormat[T: JsonFormat] = jsonFormat2(VkResponse.apply[T])
  implicit def vkResponseUnmarsh[T: JsonFormat] = Unmarshaller[VkResponse[T]] (MediaTypes.`application/json`) {
    case HttpEntity.NonEmpty(contentType, data) => {
      data.asString.parseJson.asJsObject.fields("response").convertTo[VkResponse[T]]
    }
    case HttpEntity.Empty => ??? // TODO: Indicate error
  }

}
