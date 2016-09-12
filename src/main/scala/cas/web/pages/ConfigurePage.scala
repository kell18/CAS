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
import org.joda.time.{Period, Duration}
import spray.client.pipelining._
import akka.pattern.ask
import spray.json._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import cas.utils.StdImplicits._
import cas.utils.UtilAliases.ErrorMsg
import cas.web.dealers.DealersFactory
import scala.concurrent.Await
import scala.concurrent.duration._

// TODO: Rename to vk auth
object ConfigurePage {
  import VkApiDealer._
  import cas.web.interface.ImplicitRuntime._
  import system.dispatcher
  import cas.service.AServiceControl.{GetStatus, Start, Status, Stop}
  implicit val timeout = Timeout(3.seconds)
  val searcher = new ElasticSearch("http://localhost:9201", "rbc-posts", "posts")

  /*new Period().plusSeconds(10) ->  1.0)*/

	def apply(pagePath: String, serviceControl: ActorRef) = path(pagePath){
		get	{
			parameter("isRun".as[Boolean].?) { (isRunOpt) =>
        val errOrEstim = createEstimator(
          LoyaltyConfigs(Map(
            Duration.standardMinutes(5) ->  0.5,
            Duration.standardMinutes(10) -> 0.2,
            Duration.standardMinutes(15) -> 0.142857143,
            Duration.standardMinutes(20) -> 0.1),
            0.5),
          InvRelevanceConfigs(searcher, 0.121, 0.5))

        val errOrStatusOpt = for {
          isRun <- isRunOpt
        } yield for {
          dealer <- tryCreateDealer(searcher).asEitherString
          estim <- errOrEstim
          rawStatus = if (isRun) serviceControl ? Start(dealer, estim)
                      else serviceControl ? Stop
          status <- Try(Await.result(rawStatus.mapTo[Status], timeout.duration)).
            toEither.transformLeft(e => s"Internal service error: `${e.getMessage}`")
        } yield status

        val errorsOpt = for {
          errOrStatus <- errOrStatusOpt
          err <- errOrStatus.left.toOption
        } yield err

        onComplete((serviceControl ? GetStatus).mapTo[Status]) {
          case Success(status) =>
            complete(getHtml(status.status.toString, errorsOpt, pagePath))
          case Failure(NonFatal(ex)) =>
            complete(getHtml(s"Application malformed: `${ex.getMessage}`", errorsOpt, pagePath))
        }
			}
		}
	}

  def getHtml(status: String, errorsOpt: Option[ErrorMsg], path: String) = {
    <html>
      <body>
        <h2>Content Analysis System</h2>
        <span>Status: { status }</span> <br/>
        <a href={s"/$path?isRun=true"}>Start</a> <a href={s"/$path?isRun=false"}>Stop</a> <br/>
        <span>{ if (errorsOpt.isDefined) s"Some errors occurs: `${errorsOpt.get}`" }</span>
      </body>
    </html>
  }

  def createEstimator(loyaltyCfg: LoyaltyConfigs, invRelCfg: InvRelevanceConfigs) = {
    for {
      errOrInit <- Try(Await.result(invRelCfg.searcher.initStorage, 10.seconds)).
                    toEither.left.map(e => s"Unable to load estimators: `${e.getMessage}`")
      isCreated <- errOrInit
    } yield {
      if (!isCreated) system.log.warning(s"[ConfigurePage] Searching storage already exists, will use it.")
      new TotalEstimator(new LoyaltyEstimator(loyaltyCfg) :: new InvRelevanceEstimator(invRelCfg) :: Nil)
    }
  }

  def tryCreateDealer(searcher: SearchEngine) = for {
    file <- Files.readFile(Files.currentDealer)
    configs <- Try(file.parseJson.convertTo[UsingDealer])
    dealer <- DealersFactory.buildDealer(configs.id, searcher)
  } yield dealer
}