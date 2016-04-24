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
import scala.util.{Failure, Success, Try}
import spray.json._
import cas.utils.StdImplicits.RightBiasedEither
import scala.concurrent.duration._
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import scala.xml.{Node, XML}
import scala.xml.parsing.NoBindingFactoryAdapter
import Utils._
import cas.persistence.searching.SearchEngine
import scala.util.control.NonFatal
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import org.apache.commons.lang3.StringEscapeUtils.escapeEcmaScript

object VkApiDealer {
  import VkApiProtocol._

  val id = "VkApi"
  val apiUrl = "https://api.vk.com/method/"
  val apiVersion = "5.50"

  val actualityThreshold = 0.5
  val filterableCount = 10
  val queriesPerSec = 2

  val clientId = "5369112"
  val clientSecret = "fFZQIwuIPR5bXxRW0c0I"
  val scope = "wall,groups,stats,friends,offline"

  def apply(rawConfigs: String, searcher: SearchEngine)(implicit system: ActorSystem) = for {
    cfg <- Try(rawConfigs.parseJson.convertTo[VkApiConfigs])
  } yield new VkApiDealer(cfg, searcher)
}

class VkApiDealer(cfg: VkApiConfigs, searcher: SearchEngine)(implicit val system: ActorSystem) extends ContentDealer {
  import VkApiProtocol._
  import VkApiDealer._
  import system.dispatcher
  val ind = "rbc"
  val shape = "posts"

  val postsToSift = new LinkedBlockingQueue[VkPost]()
  val defaultParams = "owner_id" -> cfg.ownerId.toString :: "v" -> apiVersion :: Nil

  override def estimatedQueryFrequency = 1.second / queriesPerSec

  var postsPerQuery = 3
  var startPostInd = -postsPerQuery

  /*def pullSubjects: Future[Either[ErrorMsg, Subjects]] = {
    val pipeline = sendReceive ~> unmarshal[VkFallible[VkResponse[VkPostsComments]]]
    startPostInd = Math.min((startPostInd + postsPerQuery) % filterableCount, filterableCount)
    for {
      resp <- pipeline(Get(buildRequest("execute", "access_token" -> cfg.token :: "v" -> apiVersion ::
        "code" -> escapeEcmaScript(commentsQuery(startPostInd, postsPerQuery)) :: Nil)))
    } yield for {
      postsComments <- resp.errorOrResp.transform(_.getMessage, _.items)
    } yield for {
      VkPostsComments(post, comments) <- postsComments
      _ = searcher.pushEntity(post.id.toString, post.text)
      comment <- comments
    } yield Subject(List(
      ID(comment.id.toString),
      Subject(List(ID(post.id.toString))),
      Likability(comment.likes.count.toDouble),
      CreationDate(new DateTime(comment.date * sec2Millis)),
      Description(comment.text)
    ))
  }*/

  override def pullSubjectsChunk: Future[Either[ErrorMsg, Subjects]] = {
    val pipeline = sendReceive ~> unmarshal[VkFallible[VkResponse[VkComment]]]
    for {
      errOrPost <- pullTopPost
    } yield for {
      post <- errOrPost
      errOrResp <- Try(Await.result(pipeline(Get(buildRequest("wall.getComments", "post_id" -> post.id.toString ::
        "need_likes" -> "1" :: defaultParams))), 10.seconds)).toEither.left.map(e => s"That error... `$e`")  // TODO: Rm
      resp <- errOrResp.errorOrResp.left.map(_.getMessage)
    } yield for {
      comment <- resp.items
      date = new DateTime(comment.date * sec2Millis)
      /*_ = println("PastSecs: " + new Period(date, DateTime.now()).toStandardSeconds.getSeconds +
                "Date: " + date +
                "Comment: " + comment.text)*/
    } yield Subject(List(
        ID(comment.id.toString),
        Subject(List(ID(post.id.toString))),
        Likability(comment.likes.count.toDouble),
        CreationDate(new DateTime(comment.date * sec2Millis)),
        Description(comment.text)
    ))
  }

  // TODO: Mb remove
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
     _ = logEstim(estim, "Deleting comment")
    } yield buildDelLine(cfg.ownerId.toString, id.value)).map(_.right.getOrElse("")).mkString // TODO: Rm
    if (scriptLines.nonEmpty) for {
      resp <- pipeline(Get(buildRequest("execute","access_token" -> cfg.token ::
        "v" -> apiVersion :: "code" -> (scriptLines.mkString + "return%201;") :: Nil)))
    } yield for {
      vkResp <- resp.errorOrResp.left.map(_.getMessage)
    } yield vkResp
    Future { Right(VkSimpleResponse(1)) }
  }

  // TODO: Make simple queue of indexes
  def pullTopPost = {
    import java.util.concurrent.TimeUnit
    import spray.json.DefaultJsonProtocol // TODO: Rm
    if (!postsToSift.isEmpty) {
      val post = postsToSift.poll(1000, TimeUnit.MILLISECONDS) // TODO: Mb problem here - concurrent queue access
      searcher.pushEntity(post.id.toString, post.getFullText) onComplete {
        case Success(Left(err)) => println(s"Cannot push entity to searcher: $err")
        case Failure(ex) => println(s"Cannot push entity to searcher: ${ex.getMessage}")
        case elze => ()
      }
      Future { Right(post) }
    }
    else {
      val pipeline = sendReceive ~> unmarshal[VkFallible[VkResponse[VkPost]]]
      for {
        fallible <- pipeline(Get(buildRequest("wall.get", "count" -> filterableCount.toString :: defaultParams)))
      } yield for {
        resp <- fallible.errorOrResp.left.map(_.getMessage)
                .filter(i => i.items.nonEmpty, "VK return zero items for wall.get")
        _ = resp.items.foreach(i => postsToSift.offer(i, 1000, TimeUnit.MILLISECONDS))
      } yield {
        val topPost = postsToSift.poll(1000, TimeUnit.MILLISECONDS)
        searcher.pushEntity(topPost.id.toString, topPost.getFullText)
        topPost
      }
    }
  }

  def getPostContent(post: String) = {
    val urlRgx = """https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)""".r
    val url = urlRgx.findFirstMatchIn(post).map(_.group(0))
    println("url: " + url)
    if (url.isDefined) {
      def attributeValueEquals(value: String)(node: Node) = {
        node.attributes.exists(_.value.text == value)
      }
      val xml = loadXml(scala.io.Source.fromURL(url.get).mkString)
      val article = xml \\ "_" filter attributeValueEquals("article__text")
      println("article.text: " + article.text)
      post + " " + article.text
    }
    else post
  }

  def logEstim(estim: Estimation, action: String) = {
    println("[" + new DateTime().getHourOfDay + ":" + new DateTime().getMinuteOfHour + "] " + action +
      " with Actuality: " + estim.actuality + " " +
      " with likes cnt: " + estim.subj.getComponent[Likability].get.value +
      " with time elapsed: " + new Period(estim.subj.getComponent[CreationDate].get.value,
          DateTime.now()).toStandardSeconds.getSeconds + "sec"+
      " Text: " + estim.subj.getComponent[Description].get.text)
  }

  /*def commentsQuery(startPostInd: Long, postsCount: Int) =
    s"""
      | var posts = API.wall.get({"offset": $startPostInd, "count": $postsCount, "owner_id": ${cfg.ownerId}}).items;
      | var postComments = [];
      | var postInd = 0;
      | var fetchAmt = 100;
      | while (postInd < $postsCount) {
      |  var comments = API.wall.getComments({"owner_id": ${cfg.ownerId},
      |     "post_id": posts[postInd].id, "count": fetchAmt, "need_likes": 1});
      |  var fetchedComments = comments.items.length;
      |  while (fetchedComments < comments.count) {
      |   comments.items = comments.items + (API.wall.getComments({"owner_id": ${cfg.ownerId},
      |     "offset": fetchedComments, "post_id": posts[postInd].id, "count": fetchAmt, "need_likes": 1}).items);
      |   fetchedComments = fetchedComments + fetchAmt;
      |  }
      |   postComments.push({
      |     "post": posts[postInd],
      |     "comments": comments.items
      |   });
      |   postInd = postInd + 1;
      | }
      | return { "count": postComments.length, "items": postComments };
    """.stripMargin*/


  def buildDelLine(ownerId: String, id: String) =
    s"""API.wall.deleteComment({%22owner_id%22:%22$ownerId%22,%22comment_id%22:$id});"""

  def buildRequest(method: String, params: List[(String, String)]) = Web.buildRequest(apiUrl)(method)(params)
}

