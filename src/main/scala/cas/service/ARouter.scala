package cas.service

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.pipe
import cas.analysis.subject.Subject
import cas.persistence.searching.ElasticSearch
import cas.utils.RemoteLogger
import cas.utils.UtilAliases._

import scala.collection.mutable
import scala.collection.mutable.Queue

object ARouter {
  case object PullSubjects
  case class PulledSubjects(subjs: Subjects)
  case class PushingEstimations(estims: Estimations)

  case class Estimation(subj: Subject, actuality: Double)
}

class ARouter(producer: ActorRef) extends Actor with ActorLogging {
  import ARouter._

  val pulledSubjs = mutable.Queue.empty[PulledSubjects]
  val waitingWorkers = mutable.Queue.empty[ActorRef]

  override def preStart = {
    super.preStart()
    producer ! PullSubjects
  }

  override def receive = {
    case PullSubjects => {
      RemoteLogger.info("PullSubjects `" + pulledSubjs.mkString + "`")
      if (pulledSubjs.isEmpty) waitingWorkers.enqueue(sender)
      else {
        sender ! pulledSubjs.dequeue
        producer ! PullSubjects
      }
    }

    case PulledSubjects(chunk) => {
      RemoteLogger.info("PulledSubjects: `" + chunk.mkString + "`")
      RemoteLogger.info("WaitingWorkers: `" + waitingWorkers.mkString + "`")
      if (waitingWorkers.isEmpty) pulledSubjs.enqueue(PulledSubjects(chunk))
      else {
        waitingWorkers.dequeue ! PulledSubjects(chunk)
        producer ! PullSubjects
      }
    }

    case estims: PushingEstimations => {
      RemoteLogger.info("PushingEstimations: `" + estims.estims.mkString + "`")
      producer forward estims
    }
  }
}
