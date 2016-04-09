package cas.service

import akka.actor.{Actor, ActorRef}
import akka.pattern.pipe
import cas.analysis.subject.Subject
import cas.utils.UtilAliases._
import scala.collection.mutable
import scala.collection.mutable.Queue

object ARouter {
  case object PullSubjects
  case class PulledSubjects(subjs: Subjects)
  case class PushingEstimations(estims: Estimations)

  case class Estimation(subj: Subject, actuality: Double)
}

class ARouter(producer: ActorRef) extends Actor {
  import ARouter._

  val pulledSubjs = mutable.Queue.empty[PulledSubjects]
  val waitingWorkers = mutable.Queue.empty[ActorRef]

  override def preStart = {
    super.preStart()
    producer ! PullSubjects
  }

  override def receive = {
    case PullSubjects => {
      //  println("[Rout] PullSubjects `" + pulledSubjs.mkString + "`")
      if (pulledSubjs.isEmpty) waitingWorkers.enqueue(sender)
      else {
        sender ! pulledSubjs.dequeue
        producer ! PullSubjects
      }
    }

    case PulledSubjects(chunk) => {
      // println("[Rout] waitingWorkers: `" + waitingWorkers.mkString + "`")
      if (waitingWorkers.isEmpty) pulledSubjs.enqueue(PulledSubjects(chunk))
      else {
        waitingWorkers.dequeue ! PulledSubjects(chunk)
        producer ! PullSubjects
      }
    }

    case estims: PushingEstimations => {
      // println("[Rout] PushingEstimations: `" + estims.estims.mkString + "`")
      producer forward estims
    }
  }
}
