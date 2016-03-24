package cas.web.dealers

import akka.actor.ActorSystem
import cas.analysis.subject.components.ID
import cas.service.{ContentDealer, Estimation}
import cas.subject.Subject
import cas.subject.components._
import cas.utils.Extractors._
import cas.utils.Web
import spray.client.pipelining._

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration._
import scala.concurrent.duration.DurationDouble
import scala.util.parsing.json.JSON
import scala.util.{Failure, Success}

object VkConfigs {
  val apiUrl = "https://api.vk.com/method/"
  val apiVersion = "5.7"
  val queriesPerSec = 5.0
  val threshold = 0.3
  val siftCount = 10
  val clientId = "5369112"
  val clientSecret = "fFZQIwuIPR5bXxRW0c0I"
  val scope = "wall,groups,stats,friends"
}

class VkApiDealer(val ownerId: Long, val token: Option[Long])(implicit val system: ActorSystem) extends ContentDealer {
  import VkApiProtocol._
  import VkConfigs._
  import system.dispatcher

  val postsToSift = new mutable.Queue[VkPost]()
  val pipeline = sendReceive
  val defaultParams = "owner_id" -> "51730148" :: "v" -> apiVersion :: Nil
  val tokenF: Future[String] = {
    for {
      resp <- pipeline(Get(Web.buildRequest("https://oauth.vk.com/")("access_token")("grant_type" -> "client_credentials"
        :: "client_id" -> clientId :: "client_secret" -> clientSecret :: "scope" -> "wall,groups,stats,friends"
        :: defaultParams)))
      token <- for {
        Some(Dict(respJson)) <- List(JSON.parseFull(resp.entity.asString))
        Str(token) = respJson("access_token")
      } yield token
    } yield token
  }

  override def estimatedQueryFrequency = (1000.0 / queriesPerSec).millisecond

  override def pullSubjectsChunk: Future[List[Subject]] = {
    val subjPipe = sendReceive ~> unmarshal[VkResponse[VkComment]]
    for {
      post <- pullTopPost
      token <- tokenF
      _ = println(buildRequest("wall.getComments", "access_token" -> token :: "post_id" -> post.id.toString ::
        "need_likes" -> "1" :: defaultParams))

      vkResp <- subjPipe(Get(buildRequest("wall.getComments", "access_token" -> token :: "post_id" -> post.id.toString ::
        "need_likes" -> "1" :: defaultParams)))
      subjects = for {
        vkCmt <- vkResp.items
      } yield Subject(List(
              ID(vkCmt.id.toString),
              Subject(List(ID(post.id.toString))),
              Likability(vkCmt.likes.count.toDouble),
              Description(vkCmt.text)
            ))
    } yield subjects
  }

  override def pushEstimation(estim: Estimation): Future[Any] = {
    val pipeline = sendReceive ~> unmarshal[VkResponse[VkComment]]
    // TODO: Delete comments
    if (estim.actuality < threshold) {
      val cmps = for {
        id <- estim.subj.getComponent[ID].right
        post <- estim.subj.getComponent[Subject].right
        postId <- post.getComponent[ID].right
      } yield (id.value, postId.value)
      if (cmps.isLeft) Future { Failure(new NoSuchElementException(cmps.left.get)) }
      else {
        val id = cmps.right.get._1
        val postId = cmps.right.get._2
        val responce = for {
          token <- tokenF
          resp <- pipeline(Get(buildRequest("wall.getComments", "access_token" -> token :: "start_comment_id" -> id ::
            "post_id" -> postId :: "count" -> "1" :: defaultParams)))
        } yield resp
        responce onFailure { case ex: Throwable => println("pushEstimation Fail: " + ex.getLocalizedMessage) }
        responce.foreach(r => r.items.foreach(c => println("Txt: " + c.text)))
        responce
      }
    }
    else Future { Unit } // Leave comment
  }

  override def estimateChunkLim: Future[Double] = for {
    resp <- pipeline(Get(buildRequest("wall.get", "count" -> siftCount.toString :: "offset" -> "3" :: defaultParams)))
    commentsCnt = for {
      Some(Dict(respJson)) <- List(JSON.parseFull(resp.entity.asString))
      Dict(respObj) = respJson("response")
      Lst(posts) = respObj("items")
      Dict(post) <- posts
      Dict(comments) = post("comments")
      Dbl(count) = comments("count")
    } yield count
  } yield commentsCnt.sum / commentsCnt.length

  def pullTopPost = {
    if (postsToSift.nonEmpty) Future { postsToSift.dequeue }
    else {
      val postsPipe = sendReceive ~> unmarshal[VkResponse[VkPost]]
      val respF = postsPipe(Get(buildRequest("wall.get", "count" -> siftCount.toString :: defaultParams)))
      respF map { vkResp =>
        vkResp.items.foreach(postsToSift.enqueue(_))
        postsToSift.dequeue
      }
    }
  }

  def buildRequest(method: String, params: List[Pair[String, String]]) = Web.buildRequest(apiUrl)(method)(params)
}

