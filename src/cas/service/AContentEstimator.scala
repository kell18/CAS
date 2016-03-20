package cas.service

import akka.actor.{Actor, ActorRef}
import akka.pattern.pipe
import cas.estimation.TotalEstimator
import cas.subject._

import scala.concurrent.Future

class AContentEstimator(estimator: TotalEstimator, router: ActorRef) extends Actor {
  import RoutingScheme._
  import cas.web.interface.ImplicitActorSystem._
  import system.dispatcher

  override def preStart(): Unit = {
    super.preStart()
    router ! PullSubjects
  }

  override def receive = {
    case PulledSubjects(subjs) => {
      Future { makeEstimations(subjs) }.map(PushEstimations).pipeTo(sender())
    }
  }

  def makeEstimations(subjs: List[Subject]): List[Estimation] = for {
    subj <- subjs
    estim = estimator.estimateActuality(subj)
  } yield Estimation(subj, estim)
}
