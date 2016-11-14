package cas.web.pages

import spray.routing._
import Directives._
import akka.actor.ActorRef
import akka.util.Timeout
import cas.analysis.estimation._
import cas.persistence.searching.{ElasticSearch, SearchEngine}
import cas.utils.{Files, Web}
import cas.web.dealers.vk.VkApiDealer
import cas.web.model.UsingDealerProtocol._
import cas.web.model._
import org.joda.time.{Duration, Period}
import spray.client.pipelining._
import akka.pattern.ask
import cas.analysis.subject.Subject
import cas.analysis.subject.components.{Attachments, Likability, Relevance}
import cas.math.Mathf._
import spray.json._

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import cas.utils.StdImplicits._
import cas.utils.UtilAliases.ErrorMsg
import cas.web.dealers.DealersFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import cas.web.pages.templates.Templates._

import scala.collection.mutable

// TODO: Rename to vk auth
object ControlPage {
  import VkApiDealer._
  import cas.web.interface.ImplicitRuntime._
  import system.dispatcher
  import cas.service.AServiceControl.{GetStatus, Start, Status, Stop}
  implicit val timeout = Timeout(60.seconds)
  val searcher = new ElasticSearch("http://localhost:9201", "content-ind", "posts")

  var useAttachments = false
  var useLikes = false
  var useRelevance = false
  var useCorrectness = false

  /*new Period().plusSeconds(10) ->  1.0)*/

	def apply(pagePath: String, serviceControl: ActorRef) = path(pagePath) {
		get	{
			parameter("useAttachments".as[Boolean].?, "useLikes".as[Boolean].?, "useRelevance".as[Boolean].?,
        "useCorrectness".as[Boolean].?, "isFormSent".as[Boolean].?) {
      (attachmentsOpt, likesOpt, relevanceOpt, correctnessOpt, isFormSent) =>
        val isTurnOf = attachmentsOpt.isEmpty && likesOpt.isEmpty && relevanceOpt.isEmpty &&
          correctnessOpt.isEmpty && isFormSent.isEmpty

        if (isTurnOf) {
          onComplete((serviceControl ? GetStatus).mapTo[Status]) {
            case Success(status) => complete(getHtml(status.status.toString, pagePath))
            case Failure(NonFatal(ex)) => complete(getHtml(s"Application malformed: `${ex.getMessage}`", pagePath))
          }
        } else {
          useAttachments = attachmentsOpt.isDefined && attachmentsOpt.get
          useLikes = likesOpt.isDefined && likesOpt.get
          useRelevance = relevanceOpt.isDefined && relevanceOpt.get
          useCorrectness = correctnessOpt.isDefined && correctnessOpt.get

          var attachEstimOpt: Option[AttachmentsEstimator] = None
          val systems = new mutable.ListBuffer[ActualityEstimator]()

          if (useAttachments) attachEstimOpt = Some(new AttachmentsEstimator(new AttachmentsConfigs))
          if (useLikes) systems += new LoyaltyEstimator(LoyaltyConfigs(Map(
            Duration.standardSeconds(5) ->  0.5,
            Duration.standardMinutes(10) -> 0.2,
            Duration.standardMinutes(15) -> 0.142857143,
            Duration.standardMinutes(20) -> 0.1),
            0.33))
          if (useRelevance) systems += new InvRelevanceEstimator(InvRelevanceConfigs(searcher, 0.055, 0.33))
          if (useCorrectness) systems += new CorrectnessEstimator(CorrectnessConfigs(weight = 0.33))


          val estim = new TotalEstimator(systems.toList, attachEstimOpt)

          val isAnyInitErrorsOpt = (for {
            isSearcherInit <- Try(Await.result(searcher.initStorage, 10.seconds)).
                        toEither.left.map(e => s"Unable to init searcher: `${e.getMessage}`")
            _ <- isSearcherInit
            dealer <- tryCreateDealer(searcher).asEitherString
            rawStatus = serviceControl ? Start(dealer, estim)
            status <- Try(Await.result(rawStatus.mapTo[Status], timeout.duration)).
              toEither.transformLeft(e => s"Internal service error: `${e.getMessage}`")
          } yield status).left.toOption

          onComplete((serviceControl ? GetStatus).mapTo[Status]) {
            case Success(status) =>
              complete(getHtml(status.status.toString, pagePath, isAnyInitErrorsOpt))
            case Failure(NonFatal(ex)) =>
              complete(getHtml(s"Application malformed: `${ex.getMessage}`", pagePath, isAnyInitErrorsOpt))
          }
        }
			}
		}
	}

  // TODO: Separate view and model
  def getHtml(status: String, path: String, isInitErrorsOpt: Option[ErrorMsg] = None) = defaultTemplate { <span>
      <h3 class="mb20">Status: { status }</h3>
      <form action={path} method="get">
        <input type="hidden" name="isFormSent" value="true" />
        <div class="mb10">Components:</div>

        <label><input type="checkbox" name="useAttachments" class="mb10" checked={useAttachments.toOption(xml.Text(""))} />
          Use attachments</label> <br/>
        <label><input type="checkbox" name="useLikes" class="mb10" checked={useLikes.toOption(xml.Text(""))} />
          Use likes</label> <br/>
        <label><input type="checkbox" name="useRelevance" class="mb10" checked={useRelevance.toOption(xml.Text(""))} />
          Use relevance</label> <br/>
        <label><input type="checkbox" name="useCorrectness" class="mb10" checked={useCorrectness.toOption(xml.Text(""))} />
          Use correctness</label> <br/>

        <input type="submit" value="Update state" class="mb10" /> <br/>
      </form>
    <span>{ if (isInitErrorsOpt.isDefined) s"Some errors occurs: `${isInitErrorsOpt.get}`" }</span></span>

  }

  def tryCreateDealer(searcher: SearchEngine) = for {
    file <- Files.readFile(Files.currentDealer)
    configs <- Try(file.parseJson.convertTo[UsingDealer])
    dealer <- DealersFactory.buildDealer(configs.id, searcher)
  } yield dealer
}