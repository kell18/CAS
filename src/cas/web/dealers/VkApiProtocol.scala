package cas.web.dealers

import cas.subject.components.{Component, CreationDate, Description, Likability}
import scala.concurrent.Future
import spray.json.{DefaultJsonProtocol, JsonFormat}
import spray.json._
import spray.util._
import spray.http._
import spray.httpx.unmarshalling._
import spray.http.HttpResponse
import spray.httpx.unmarshalling.{FromResponseUnmarshaller, MalformedContent, Unmarshaller}
import spray.client.pipelining._
import scala.collection.immutable.Map

object VkApiProtocol extends DefaultJsonProtocol {
  case class VkResponse[T](count: Int, items: List[T])
  case class VkSimpleResponse(response: Int)
  case class VkPost(id: Int)
  case class VkComment(id: Int, from_id: Long, date: Long, text: String, likes: VkLikes)
  case class VkLikes(count: Int)
  case class VkAccessToken(accessToken: String)

  implicit val vkSimpRespJsonFormat = jsonFormat1(VkSimpleResponse)
  implicit val vkPostJsonFormat = jsonFormat1(VkPost)
  implicit val vkLikesJsonFormat = jsonFormat1(VkLikes)
  implicit val vkCommentJsonFormat = jsonFormat(VkComment, "id", "from_id", "date", "text", "likes")

  implicit def vkResponseJsonFormat[T: JsonFormat] = jsonFormat2(VkResponse.apply[T])
  implicit def vkResponseUnmarsh[T: JsonFormat] = Unmarshaller[VkResponse[T]] (MediaTypes.`application/json`) {
    case HttpEntity.NonEmpty(contentType, data) => {
      data.asString.parseJson.asJsObject.fields("response").convertTo[VkResponse[T]]
    }

    case HttpEntity.Empty => ??? // TODO: Indicate error
  }

}
