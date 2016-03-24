package cas.service

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.pipe
import cas.estimation.TotalEstimator
import cas.subject._

import scala.annotation.tailrec
import scala.concurrent.Future

class AContentEstimator(estimator: TotalEstimator, router: ActorRef) extends Actor with ActorLogging {
  import AContentRouter._
  import cas.web.interface.ImplicitActorSystem._
  import system.dispatcher

  override def preStart(): Unit = {
    super.preStart()
    router ! PullSubjects
  }

  override def receive = {
    case Subjects(chunk) => {
      Future { makeEstimations(chunk) }.map(Estimations).pipeTo(sender)
    }
    case x => log.warning("Unexpected case type in content estimator: " + x)
  }

  @tailrec
  final def makeEstimations(subjs: List[Subject], estims: List[Estimation] = Nil,
                            error: Option[String] = None): List[Estimation] = subjs match {
    case Nil => {
      if (error.isDefined) log.error(error.get)
      estims
    }
    case s::xs => {
      val estim = estimator.estimateActuality(s)
      if (estim.isRight) makeEstimations(xs, Estimation(s, estim.right.get) :: estims, error)
      else {
        // TODO: May be compare errors and log different
        makeEstimations(xs, estims, Some(estim.left.get))
      }
    }
  }
}
