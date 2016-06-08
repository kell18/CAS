package cas.web.pages

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import cas.analysis.estimation._
import cas.persistence.searching.{ElasticSearch, SearchEngine}
import cas.utils.Files
import cas.utils.StdImplicits._
import cas.utils.UtilAliases.ErrorMsg
import cas.web.dealers.DealersFactory
import cas.web.model.UsingDealerProtocol._
import cas.web.model._
import org.joda.time.Period
import spray.json._
import spray.routing.Directives._
import spray.routing._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

// TODO: Rename to vk auth
object ExitPage {
  import cas.service.AServiceControl.{GetStatus, Start, Status, Stop}
  import cas.web.interface.ImplicitRuntime._
  import system.dispatcher
  implicit val timeout = Timeout(3.seconds)


	def apply(pagePath: String) = path(pagePath){
		get	{
			parameter("isStop".as[Boolean].?) { (isStopOpt) =>
        val response = if (isStopOpt.isDefined && isStopOpt.get) {
          system.shutdown()
          "Stopping app..."
        } else "App is running."
        complete(response)
			}
		}
	}
}