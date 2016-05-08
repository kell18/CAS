package cas.service

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.pipe
import cas.analysis.subject.Subject
import cas.persistence.searching.ElasticSearch
import cas.utils.RemoteLogger
import cas.utils.UtilAliases._
import cas.web.dealers.vk.VkApiProtocol.VkPost

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

  val pulledSubjs = new LinkedBlockingQueue[PulledSubjects]()
  val waitingWorkers = new LinkedBlockingQueue[ActorRef]()

  override def preStart = {
    super.preStart()
    /*for (i <- 1 to 10) {
      println("I : " + i)*/
    producer ! PullSubjects
  }

  override def receive = {
    case PullSubjects => {
      // RemoteLogger.info("PullSubjects `" + pulledSubjs.mkString + "`")
      // log.info("PullSubjects request")
      if (pulledSubjs.isEmpty) waitingWorkers.offer(sender, 1000, TimeUnit.MILLISECONDS)
      else {
        sender ! pulledSubjs.poll(1000, TimeUnit.MILLISECONDS)
        producer ! PullSubjects
      }
    }

    case PulledSubjects(chunk) => {
      /*RemoteLogger.info("PulledSubjects: `" + chunk.mkString + "`")
      RemoteLogger.info("WaitingWorkers: `" + waitingWorkers.mkString + "`")*/
      // log.info("Subjects pulled: " + chunk)
      if (waitingWorkers.isEmpty) pulledSubjs.offer(PulledSubjects(chunk), 1000, TimeUnit.MILLISECONDS)
      else {
        waitingWorkers.poll(1000, TimeUnit.MILLISECONDS) ! PulledSubjects(chunk)
        producer ! PullSubjects
      }
    }

    case estims: PushingEstimations => {
      // RemoteLogger.info("PushingEstimations: `" + estims.estims.mkString + "`")
      // log.info("Estimations computed: " + estims.estims)
      producer forward estims
    }
  }
}
