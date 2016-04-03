package cas.web.dealers.vk

import akka.actor.ActorSystem
import cas.analysis.subject.Subject
import cas.analysis.subject.Subject.Subjects
import cas.analysis.subject.components._
import cas.service.{ContentDealer, Estimation}
import cas.utils.Web
import cas.utils.Mathf.sec2Millis
import cas.web.dealers.vk.VkApiProtocol._
import cas.utils.StdImplicits._
import cas.utils.Utils._
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._ // Don't dell!
import org.joda.time.DateTime
import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.util.Try
import spray.json._
import scala.concurrent.duration._

object VkApiDealer {
  import VkApiProtocol._

  val id = "VkApi"
  val apiUrl = "https://api.vk.com/method/"
  val apiVersion = "5.50"

  val actualityThreshold = 0.3
  val filterableCount = 10
  val queriesPerSec = 2

  val clientId = ""
  val clientSecret = ""
  val scope = "wall,groups,stats,friends"

  def apply(rawConfigs: String)(implicit system: ActorSystem) = for {
    c <- Try(rawConfigs.parseJson.convertTo[VkApiConfigs])
  } yield new VkApiDealer(c)
}

class VkApiDealer(cfg: VkApiConfigs)(implicit val system: ActorSystem) extends ContentDealer {
  import VkApiProtocol._
  import VkApiDealer._
  import system.dispatcher

  val postsToSift = new mutable.Queue[VkPost]()
  val defaultParams = "owner_id" -> cfg.ownerId.toString :: "v" -> apiVersion :: Nil

  override def estimatedQueryFrequency = 1.second / queriesPerSec

  override def pullSubjectsChunk: Future[Either[ErrorMsg, Subjects]] = {
    val pipeline = sendReceive ~> unmarshal[VkFallible[VkResponse[VkComment]]]
    for {
      errorOrPost <- pullTopPost
    } yield for {
      post <- errorOrPost.left.map(_.getMessage)
      respF = Await.result(pipeline(Get(buildRequest("wall.getComments", "post_id" -> post.id.toString ::
        "need_likes" -> "1" :: defaultParams))), 10.seconds)
      resp <- respF.errorOrResp.left.map(_.getMessage)
      _ = println("resp: " + resp)
    } yield for {
      comment <- resp.items
    } yield Subject(List(
        ID(comment.id.toString),
        Subject(List(ID(post.id.toString))),
        Likability(comment.likes.count.toDouble),
        CreationDate(new DateTime(comment.date * sec2Millis)),
        Description(comment.text)
    ))
  }

  override def pushEstimation(estimation: Estimation): Future[Either[ErrorMsg, Any]] = Future {
    val pipeline = sendReceive ~> unmarshal[VkFallible[VkSimpleResponse]]
    if (estimation.actuality < actualityThreshold) {
      for {
        id <- estimation.subj.getComponent[ID]
        post <- estimation.subj.getComponent[Subject]
        postId <- post.getComponent[ID]
        respF <- Try(Await.result(pipeline(Get(buildRequest("wall.deleteComment", "access_token" -> cfg.token ::
          "comment_id" -> id.value :: defaultParams))), 10.seconds)).toEither.left.map(_.getMessage)
        resp <- respF.errorOrResp.left.map(_.getMessage)
      } yield resp
    }
    else Right(VkSimpleResponse(1))
  }

  def pullTopPost = {
    if (postsToSift.nonEmpty) Future { Right(postsToSift.dequeue) }
    else {
      val pipeline = sendReceive ~> unmarshal[VkFallible[VkResponse[VkPost]]]
      for {
        respF <- pipeline(Get(buildRequest("wall.get", "count" -> filterableCount.toString :: defaultParams)))
      } yield for {
        resp <- respF.errorOrResp
      } yield {
        resp.items.tail.foreach(postsToSift.enqueue(_))
        resp.items.head
      }
    }
  }

  def buildRequest(method: String, params: List[(String, String)]) = Web.buildRequest(apiUrl)(method)(params)
}

