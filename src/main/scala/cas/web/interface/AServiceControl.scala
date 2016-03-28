package cas.web.interface

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import cas.analysis.estimation.{LoyaltyConfigs, LoyaltyEstimator, TotalEstimator}
import cas.service.{AContentService, ContentDealer}
import cas.web.dealers.DealersFactory
import cas.web.model.UsingDealer
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object AServiceControl {
  case class Start(dealerID: UsingDealer)
  object Stop

  def serviceProps(dealer: ContentDealer, estimator: TotalEstimator) = Props(new AContentService(dealer, estimator))
}

class AServiceControl extends Actor with ActorLogging {
  import ImplicitActorSystem._
  import AServiceControl._

  val estimator = new TotalEstimator(new LoyaltyEstimator(LoyaltyConfigs(1.0, 1.0)) :: Nil)

  override def receive: Receive = serve(None)

  def serve(service: Option[ActorRef]): Receive = {
    case Start(dealerId) => {
      service.foreach(system.stop)
      DealersFactory.buildDealer(dealerId.id) match  {
        case Success(d) => context.become(serve(Some(system.actorOf(serviceProps(d, estimator)))))
        case Failure(NonFatal(e)) => log.error(s"[AServiceControl] Cannot create content service: `${e.getMessage}`")
      }

    }
    case Stop => {
      service.foreach(system.stop)
      context.become(serve(None))
    }
  }
}
