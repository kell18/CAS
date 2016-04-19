package cas.web.interface

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import cas.analysis.estimation.{LoyaltyConfigs, LoyaltyEstimator, TotalEstimator}
import cas.service.{AProducer, AServiceControl, ContentDealer}
import cas.web.dealers.DealersFactory
import cas.web.model.UsingDealer
import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.util.LoggingContext
import spray.http.StatusCodes._
import cas.web.pages._

import scala.util.{Failure, Success}
import scala.util.control.NonFatal

object PageUrls {
  val index = "index"
  val auth = "auth"
  val authVk = "auth-vk"
}

trait InterfaceControl extends HttpService {
  import PageUrls._
  import ImplicitRuntime._

  val serviceControl = system.actorOf(Props[AServiceControl])

  def route = respondWithMediaType(`text/html`) {
    IndexPage("", serviceControl) ~
    VkAuth(authVk) ~
    ConfigurePage("configure", serviceControl) ~
    TestPage("t")
  }

  implicit def commonExceptionHandler(implicit log: LoggingContext) = ExceptionHandler {
    case NonFatal(ex) => {
      println("Unhandled error occurs. Exception: `" + ex.getMessage + "`")
      complete(StatusCodes.InternalServerError, "Something goes wrong, try to reload last page.")
    }
  }
}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class AInterfaceControl extends Actor with InterfaceControl {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  override def receive = runRoute(route)

//  def serve(service: Option[ActorRef]): Receive = {
//    case Start(dealerId) => {
//      service.foreach(context.stop)
//      DealersFactory.buildDealer(dealerId.id) match  {
//        case Success(dealer) => {
//          sender ! Success
//          changeContext(serve(Some(context.actorOf(serviceProps(dealer, estimator)))))
//        }
//        case Failure(ex) => {
//          sender ! Failure(ex)
//          log.error("[AServiceControl] Cannot create content service: `" + ex.getMessage + "`")
//        }
//      }
//    }
//
//    case Stop => {
//      service.foreach(context.stop)
//      changeContext(serve(None))
//    }
//  }

}

  // def serviceProps(dealer: ContentDealer, estimator: TotalEstimator) = Props(new AContentService(dealer, estimator))
