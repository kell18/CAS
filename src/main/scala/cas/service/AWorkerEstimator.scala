package cas.service

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.pipe
import cas.analysis.estimation._
import cas.analysis.subject._
import cas.analysis.subject.components.Description

import scala.annotation.tailrec
import scala.concurrent.Future

class AWorkerEstimator(estimator: ActualityEstimator, router: ActorRef) extends Actor with ActorLogging {
  import ARouter._
  import cas.web.interface.ImplicitRuntime._
  import system.dispatcher

  override def preStart(): Unit = {
    super.preStart()
    router ! PullSubjects
  }

  override def receive = {
    case PulledSubjects(chunk) => {
      Future { makeEstimations(chunk) }.map(PushingEstimations).pipeTo(sender)
      router ! PullSubjects
    }
    case x => log.warning("[AContentEstimator] Unexpected case type: " + x)
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
