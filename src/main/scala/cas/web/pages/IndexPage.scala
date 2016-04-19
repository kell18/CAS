package cas.web.pages

import scala.io.Source
import scala.util.{Failure, Success, Try}
import spray.routing._
import spray.json._
import Directives._
import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import cas.analysis.estimation._
import cas.persistence.searching.{ElasticSearch, SearchEngine}
import cas.service.{AProducer, AServiceControl}
import cas.utils._
import cas.web.dealers.DealersFactory
import cas.web.interface.ImplicitRuntime
import cas.web.model._
import org.joda.time.Period
import spray.json._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal
import cas.utils.StdImplicits.RightBiasedEither

object IndexPage {
  import cas.web.interface.ImplicitRuntime._
  import scala.concurrent.ExecutionContext.Implicits.global
  import AServiceControl._
  implicit val timeout = Timeout(3.seconds)

	def apply(pagePath: String, serviceControl: ActorRef) = path(pagePath) {
    get {
      onComplete((serviceControl ? GetStatus).mapTo[Status]) {
        case Success(serviceStat) => complete(getHtml(serviceStat.status.toString))
        case Failure(NonFatal(ex)) => complete(getHtml(s"Application malformed: `${ex.getMessage}`"))
      }
    }
  }

  def getHtml(status: String) = {
    <html>
      <body>
        <h2>Content Analysis System</h2>
        <span>Status: { status }</span>
      </body>
    </html>
  }
}