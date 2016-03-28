package cas.service

import akka.actor.{Actor, ActorRef}
import akka.pattern.pipe
import cas.analysis.subject.Subject
import scala.collection.mutable
import scala.collection.mutable.Queue

object AContentRouter {
  case object PullSubjects
  case class Subjects(subjs: List[Subject])
  case class Estimations(estims: List[Estimation])
}

class AContentRouter(producer: ActorRef) extends Actor {
  import AContentRouter._

  val pulledSubjs = mutable.Queue.empty[Subjects]
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

    case Subjects(chunk) => {
      // println("Rout - Subjects - waitingWorkers: " + waitingWorkers.length)
      if (waitingWorkers.isEmpty) pulledSubjs.enqueue(Subjects(chunk))
      else {
        waitingWorkers.dequeue ! Subjects(chunk)
        producer ! PullSubjects
      }
    }

    case Estimations(estims) => producer forward Estimations(estims)
  }
}
