package cas.web.dealers.vk

import akka.actor.ActorSystem
import cas.analysis.subject.Subject
import cas.analysis.subject.components._
import cas.service.ARouter.Estimation
import cas.service.ContentDealer
import cas.utils.{Utils, Web}
import cas.utils.Mathf.sec2Millis
import cas.web.dealers.vk.VkApiProtocol._
import cas.utils.StdImplicits._
import cas.utils.UtilAliases._
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import org.joda.time.{DateTime, Period}
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

  val actualityThreshold = 1.0
  val filterableCount = 30
  val queriesPerSec = 1

  val clientId = "5369112"
  val clientSecret = "fFZQIwuIPR5bXxRW0c0I"
  val scope = "wall,groups,stats,friends,offline"

  def apply(rawConfigs: String)(implicit system: ActorSystem) = for {
    c <- Try(rawConfigs.parseJson.convertTo[VkApiConfigs])
  } yield new VkApiDealer(c)
}

class VkApiDealer(cfg: VkApiConfigs)(implicit val system: ActorSystem) extends ContentDealer {
  import VkApiProtocol._
  import VkApiDealer._
  import system.dispatcher

   // scope = "wall,groups,stats,friends"
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
    } yield for {
      comment <- resp.items
//      _ = println("Pull: " +
//            comment.text +
//            new Period(new DateTime(comment.date * sec2Millis, Utils.timeZone), DateTime.now(Utils.timeZone)).toStandardSeconds)
//      _ = if (comment.text.startsWith("_test")) println("Pull: " +
//        comment.text +
//        new Period(new DateTime(comment.date * sec2Millis, Utils.timeZone), DateTime.now(Utils.timeZone)).toStandardSeconds
//      ) else {}
    } yield Subject(List(
        ID(comment.id.toString),
        Subject(List(ID(post.id.toString))),
        Likability(comment.likes.count.toDouble),
        CreationDate(new DateTime(comment.date * sec2Millis, Utils.timeZone)),
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
        _ = println("Deleting cmt: " + estimation.subj.getComponent[Description].get.text +
          " with likes cnt: " + estimation.subj.getComponent[Likability].get.value +
          " with time elapsed: " + new Period(estimation.subj.getComponent[CreationDate].get.value, DateTime.now(Utils.timeZone)).toStandardSeconds.getSeconds + "sec")
        respF <- Try(Await.result(pipeline(Get(buildRequest("wall.deleteComment", "access_token" -> cfg.token ::
          "comment_id" -> id.value :: defaultParams))), 10.seconds)).toEither.left.map(_.getMessage)
        resp <- respF.errorOrResp.left.map(_.getMessage)
      } yield VkSimpleResponse(1)
    }
    else Right(VkSimpleResponse(1))
  }

  override def pushEstimations(estims: Estimations): Future[Either[ErrorMsg, Any]] = {
    val pipeline = sendReceive ~> unmarshal[VkFallible[VkSimpleResponse]]
    val scriptLines = (for {
      estim <- estims if estim.actuality < actualityThreshold
    } yield for {
      id <- estim.subj.getComponent[ID]
      _ = println("[" + new DateTime().getHourOfDay + ":" + new DateTime().getMinuteOfHour + "] " +
        "Deleting cmt: " + estim.subj.getComponent[Description].get.text +
        " with likes cnt: " + estim.subj.getComponent[Likability].get.value +
        " with time elapsed: " + new Period(estim.subj.getComponent[CreationDate].get.value, DateTime.now(Utils.timeZone)).toStandardSeconds.getSeconds + "sec")
    } yield buildDelLine(cfg.ownerId.toString, id.value)).map(_.right.getOrElse("")).mkString

    if (scriptLines.nonEmpty) for {
      resp <- pipeline(Get(buildRequest("execute","access_token" -> cfg.token ::
        "v" -> apiVersion :: "code" -> (scriptLines.mkString + "return%201;") :: Nil)))
    } yield for {
      vkResp <- resp.errorOrResp.left.map(_.getMessage)
    } yield vkResp
    else Future { Right(VkSimpleResponse(1)) }
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

  def buildDelLine(ownerId: String, id: String) =
    s"""API.wall.deleteComment({%22owner_id%22:%22$ownerId%22,%22comment_id%22:$id});"""
//    escapeJava("API.wall.deleteComment({\"owner_id\":\"" + ownerId  + "\",\"comment_id\":" + id + "});")

  def buildRequest(method: String, params: List[(String, String)]) = Web.buildRequest(apiUrl)(method)(params)
}

