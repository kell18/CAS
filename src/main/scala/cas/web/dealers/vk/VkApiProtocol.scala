package cas.web.dealers.vk

import spray.http._
import spray.http.MediaTypes.`application/json`
import spray.httpx.unmarshalling.Unmarshaller
import spray.json._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import spray.httpx.ResponseTransformation._
import scala.collection.Map

object VkApiProtocol extends DefaultJsonProtocol {

  case class VkApiConfigs(token: String, ownerId: Long, siftCount: Int)
  implicit val vkApiConfigsFormat = jsonFormat3(VkApiConfigs)


  case class VkSimpleResponse(response: Int)
  implicit val vkSimpRespFormat = jsonFormat1(VkSimpleResponse)


  case class VkError(error_code: Int, error_msg: String) {
    def getMessage = s"Vk respond with error: $error_code:$error_msg "
  }
  implicit val vkErrFormat = jsonFormat2(VkError)
  implicit def vkErrUnmarsh = Unmarshaller[VkError] (`application/json`) {
    case HttpEntity.NonEmpty(contentType, data) =>
      data.asString.parseJson.asJsObject.fields("error").convertTo[VkError]

    case HttpEntity.Empty => deserializationError("Empty entity. VkError expected")
  }


  /** Represent either response or error */
  case class VkFallible[R](errorOrResp: Either[VkError, R])
  implicit def vkFallibleFormat[R: JsonFormat] = jsonFormat1(VkFallible.apply[R])
  implicit def vkFallibleUnmarsh[R: JsonFormat] = Unmarshaller[VkFallible[R]] (MediaTypes.`application/json`) {
    case HttpEntity.NonEmpty(contentType, data) => {
      Try(data.asString.parseJson.convertTo[R]) match {
        case Success(r) => VkFallible[R](Right(r))
        case Failure(NonFatal(ex)) => {
          // println("Fail to parse response: `" + ex.getMessage + "`") // TODO: RM
          vkErrUnmarsh(HttpEntity(contentType, data)) match {
            case Right(err) => VkFallible[R](Left(err))
            case Left(e) => VkFallible[R](Left(VkError(-1, // TODO: NonFatal
              s"Does't got neither response or error from Vk: ${data.asString} exception: `$e`"))) // TODO: deserErr
          }
        }
      }
    }
    case HttpEntity.Empty => deserializationError("Empty entity. VkResponse or Error expected")
  }


  case class VkResponse[T](count: Int, items: List[T])
  implicit def vkResponseFormat[T: JsonFormat] = new RootJsonFormat[VkResponse[T]]{
    def write(p: VkResponse[T]) = {
      val fields = new collection.mutable.ListBuffer[(String, JsValue)]
      fields.sizeHint(2 * 3)
      fields ++= productElement2Field[Int]("count", p, 0)
      fields ++= productElement2Field[List[T]]("items", p, 1)
      JsObject("response" -> JsObject(fields: _*))
    }
    def read(value: JsValue) = {
      val fields = value.asJsObject.fields
      if (fields.contains("response")) {
        val response = fields("response")
        val count = fromField[Int](response, "count")
        val items = fromField[List[T]](response, "items")
        VkResponse[T](count, items)
      } else deserializationError("Document not contain `responce` field.")
    }
  }


  import spray.json.DefaultJsonProtocol
  case class VkPost(id: Int, text: String, repost: Option[List[VkPost]]) {
    def getFullText: String = text + " " + repost.map(_.map(_.getFullText).mkString("; ")).getOrElse("")
  }
  implicit val vkPostFormat: JsonFormat[VkPost] = lazyFormat(jsonFormat(VkPost, "id", "text", "copy_history"))


  case class VkLikes(count: Int)
  implicit val vkLikesFormat = jsonFormat1(VkLikes)


  case class VkComment(id: Int, from_id: Long, date: Long, text: String, likes: VkLikes)
  implicit val vkCommentFormat = jsonFormat(VkComment, "id", "from_id", "date", "text", "likes")


  case class VkPostsComments(post: VkPost, comments: List[VkComment])
  implicit val VkPostsCommentsFormat = jsonFormat2(VkPostsComments)
}
