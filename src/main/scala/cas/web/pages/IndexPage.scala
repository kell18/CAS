package cas.web.pages

import scala.io.Source
import scala.util.{Failure, Success, Try}
import spray.routing._
import spray.json._
import Directives._
import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import cas.analysis.estimation.{LoyaltyConfigs, LoyaltyEstimator, TotalEstimator}
import cas.service.AContentService
import cas.utils._
import cas.web.dealers.DealersFactory
import cas.web.model._
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration._

object IndexPage {
  import cas.web.interface.ImplicitActorSystem._
  import cas.web.interface.AServiceControl._
  import UsingDealerProtocol._
  implicit val timeout = Timeout(10.seconds)

	def apply(pagePath: String, serviceControl: ActorRef) = path(pagePath) {
    get {
      parameter("isRun".as[Boolean].?) { isRunOpt =>
        val contentServiceOpt = for {
          isRun <- isRunOpt
          if isRun
        } yield for {
          file <- Files.readFile(Files.currentDealer)
          currDealer <- Try(file.parseJson.convertTo[UsingDealer])
        } yield serviceControl ! Start(currDealer)

        complete {
          <html>
            <body>
              <h2>Content Analysis System</h2>
              <span>Status: </span>
              {
                if (contentServiceOpt.isDefined) {
                  if (contentServiceOpt.get.isSuccess) "Active"
                  else "Inactive (failed to run, try again)"
                } else "Inactive"
              }<br/>
              <a href="/?isRun=true">Start</a> <a href="/?isRun=false">Stop</a>
            </body>
          </html>
        }
      }
    }
  }
}