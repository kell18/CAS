package cas.web.dealers

import akka.util.Timeout
import cas.service.{Estimation, ContentDealer}
import cas.utils.Utils
import cas.subject.Subject
import cas.subject.components._
import cas.utils.Extractors._
import spray.http.{HttpRequest, HttpCharset, HttpData, HttpEntity}
import spray.http.HttpEntity.NonEmpty
import scala.collection.immutable.StringOps
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.parsing.json.JSON
import scala.util._
import scala.concurrent.duration._
import scala.collection.mutable.Queue
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.event.Logging
import akka.io.IO
import spray.json.{JsonFormat, DefaultJsonProtocol}
import spray.can.Http
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import spray.util._
import spray.json._
import DefaultJsonProtocol._

// TODO: Mae be do it with actors and context.become?
class VkApiDealer(val ownerId: Long, val token: Option[Long])(implicit val system: ActorSystem) extends ContentDealer {
  import system.dispatcher
  import VkApiJsonProtocol._

  val postsToSift = new mutable.Queue[VkPost]()
  val siftCount = 30
  val apiUrl = "https://api.vk.com/method/"
  val apiVersion = "5.7"
  val pipeline = sendReceive
  val defaultParams = ("owner_id" -> ownerId.toString) :: ("v" -> apiVersion) :: Nil

  override def estimateChunkLim: Future[Double] = for {
    resp <- pipeline(Get(buildRequest("wall.get", "count" -> siftCount.toString :: "offset" -> "3" :: defaultParams)))
    commentsCnt = for {
      Dict(respJson) <- List(resp.entity.asString.parseJson)
      Dict(respObj) = respJson("response")
      Lst(posts) = respObj("items")
      Dict(post) <- posts
      Dict(comments) = post("comments")
      Dbl(count) = comments("count")
    } yield count
  } yield commentsCnt.sum / commentsCnt.length


  override def pullSubjectsChunk: Future[List[Subject]] = {
    val subjPipe = (sendReceive ~> unmarshal[VkResponse[VkComment]])
    for {
      posts <- pullTopPosts
      post = Try { posts.dequeue } getOrElse ???
      _ = println("pullTopPosts done")
      vkResp <- subjPipe(Get(buildRequest("wall.getComments", "post_id" -> post.id.toString :: defaultParams)))
      subjects = for {
        vkCmt <- vkResp.items
      } yield new Subject(Set(
        new Likability(vkCmt.likes.toDouble),
        new Description(vkCmt.text)
      ))
    } yield subjects
  }

  override def pushEstimationsChunk(estims: List[Estimation]) = ???

  def pullTopPosts = {
    if (postsToSift.length > 0) Future { postsToSift }
    else {
      val postsPipe = (sendReceive ~> unmarshal[VkResponse[VkPost]])
      val respF = postsPipe(Get(buildRequest("wall.get", "count" -> siftCount.toString :: defaultParams)))
      respF map { vkResp =>
        vkResp.items.foreach(postsToSift.enqueue(_))
        postsToSift
      }
    }
  }

  def buildRequest(method: String, params: List[Pair[String, String]]) = apiUrl + method + (params match {
    case Nil => ""
    case p::ps => "?" + ps.foldLeft(p._1 + "=" + p._2) { (l, r) => l + "&" + r._1 + "=" + r._2 }
  })
}

