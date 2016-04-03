package cas.service

import akka.actor.{Actor, ActorRef}
import akka.pattern.pipe
import cas.analysis.subject.Subject
import cas.analysis.subject.Subject.Subjects
import cas.service.Estimation.Estimations

import scala.collection.mutable
import scala.collection.mutable.Queue

object AContentRouter {
  case object PullSubjects
  case class PulledSubjects(subjs: Subjects)
  case class PushingEstimations(estims: Estimations)
}

class AContentRouter(producer: ActorRef) extends Actor {
  import AContentRouter._

  val pulledSubjs = mutable.Queue.empty[PulledSubjects]
  val waitingWorkers = mutable.Queue.empty[ActorRef]

  override def preStart = {
    super.preStart()
    producer ! PullSubjects
  }

  override def receive = {
    case PullSubjects => {
      // println("Rout - PullSubjects - pulledSubjs: " + pulledSubjs.length)
      if (pulledSubjs.isEmpty) waitingWorkers.enqueue(sender)
      else {
        sender ! pulledSubjs.dequeue
        producer ! PullSubjects
      }
    }

    case PulledSubjects(chunk) => {
      // println("Rout - Subjects - waitingWorkers: " + waitingWorkers.length)
      if (waitingWorkers.isEmpty) pulledSubjs.enqueue(PulledSubjects(chunk))
      else {
        waitingWorkers.dequeue ! PulledSubjects(chunk)
        producer ! PullSubjects
      }
    }

    case PushingEstimations(estims) => producer forward PushingEstimations(estims)
  }
}
