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
import com.sksamuel.elastic4s.ElasticClient
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import org.joda.time.{DateTime, Period}
import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.util.Try
import spray.json._
import cas.utils.StdImplicits.RightBiasedEither
import scala.concurrent.duration._
import com.sksamuel.elastic4s.ElasticDsl._
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import scala.xml.{Node, XML}
import scala.xml.parsing.NoBindingFactoryAdapter
import Utils._

object VkApiDealer {
  import VkApiProtocol._

  val id = "VkApi"
  val apiUrl = "https://api.vk.com/method/"
  val apiVersion = "5.50"

  val actualityThreshold = 0.5
  val filterableCount = 80
  val queriesPerSec = 3

  val clientId = "5369112"
  val clientSecret = "fFZQIwuIPR5bXxRW0c0I"
  val scope = "wall,groups,stats,friends,offline"

  def apply(rawConfigs: String)(implicit system: ActorSystem, client: ElasticClient) = for {
    c <- Try(rawConfigs.parseJson.convertTo[VkApiConfigs])
  } yield new VkApiDealer(c)
}

class VkApiDealer(cfg: VkApiConfigs)(implicit val system: ActorSystem, client: ElasticClient) extends ContentDealer {
  import VkApiProtocol._
  import VkApiDealer._
  import system.dispatcher
  val ind = "rbc"
  val shape = "posts"

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
      estim <- estims
      if estim.actuality < actualityThreshold
    } yield for {
      id <- estim.subj.getComponent[ID]
     _ = logDelete(estim, "Deliting comment")
    } yield buildDelLine(cfg.ownerId.toString, id.value)).map(_.right.getOrElse("")).mkString // TODO: Rm
    if (scriptLines.nonEmpty) for {
      resp <- pipeline(Get(buildRequest("execute","access_token" -> cfg.token ::
        "v" -> apiVersion :: "code" -> (scriptLines.mkString + "return%201;") :: Nil)))
    } yield for {
      vkResp <- resp.errorOrResp.left.map(_.getMessage)
    } yield vkResp
    else Future { Right(VkSimpleResponse(1)) }
  }

  def pullTopPost = {
    if (postsToSift.nonEmpty) Future {
      val post = postsToSift.dequeue
      val i = Math.min(Math.abs(filterableCount - postsToSift.length), filterableCount-1)
      updateIndex(i.toString, post)
      Right(post)
    }
    else {
      val pipeline = sendReceive ~> unmarshal[VkFallible[VkResponse[VkPost]]]
      for {
        respF <- pipeline(Get(buildRequest("wall.get", "count" -> filterableCount.toString :: defaultParams)))
      } yield for {
        resp <- respF.errorOrResp
      } yield {
        resp.items.tail.foreach(postsToSift.enqueue(_))
        updateIndex("1", resp.items.head)
        resp.items.head
      }
    }
  }

  def updateIndex(i: String, post: VkPost) = {


    /*val upd = Try(client.execute { update id i in ind / shape doc(
    "id" -> post.id.toString,
    "text" -> getPostContent(post.text))
  })
  if (upd.isFailure) *//*client.execute { index into ind / shape id i fields(
      "id" -> post.id.toString,
      "text" -> getPostContent(post.text))
    }*/
  }

  def getPostContent(post: String) = {
    val urlRgx = """https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)""".r
    val url = urlRgx.findFirstMatchIn(post).map(_.group(0))
    // println("url: " + url)
    if (url.isDefined) {
      def attributeValueEquals(value: String)(node: Node) = {
        node.attributes.exists(_.value.text == value)
      }
      val xml = loadXml(scala.io.Source.fromURL(url.get).mkString)
      val article = xml \\ "_" filter attributeValueEquals("article__text")
      post + " " + article.text
    }
    else post
  }

  def logDelete(estim: Estimation, action: String) = {
    println("[" + new DateTime().getHourOfDay + ":" + new DateTime().getMinuteOfHour + "] " + action + "with Actuality: " + estim.actuality + " " +
      " with likes cnt: " + estim.subj.getComponent[Likability].get.value +
      " with time elapsed: " + new Period(estim.subj.getComponent[CreationDate].get.value, DateTime.now(Utils.timeZone)).toStandardSeconds.getSeconds + "sec"+
      "Text: " + estim.subj.getComponent[Description].get.text)
  }

  def buildDelLine(ownerId: String, id: String) =
    s"""API.wall.deleteComment({%22owner_id%22:%22$ownerId%22,%22comment_id%22:$id});"""
//    escapeJava("API.wall.deleteComment({\"owner_id\":\"" + ownerId  + "\",\"comment_id\":" + id + "});")

  def buildRequest(method: String, params: List[(String, String)]) = Web.buildRequest(apiUrl)(method)(params)
}

