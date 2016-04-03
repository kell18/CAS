package cas.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import cas.analysis.estimation._
import cas.service.Estimation.Estimations
import cas.utils.Utils.ErrorMsg
import scala.util.{Failure, Success}

/** Manages content flow btw dealer and router
  *
  * @param dealer concrete realization of ContentDealer
  * @param estimator accumulative estimator
  */
class AContentService(dealer: ContentDealer, estimator: TotalEstimator) extends Actor with ActorLogging {
  import AContentService._
  import AContentRouter._
  import cas.web.interface.ImplicitActorSystem._
  import system.dispatcher

  val router = context.actorOf(routerProps(self))
  val workersCount = Runtime.getRuntime.availableProcessors
  val workers = for (i <- 1 to workersCount) yield context.actorOf(workerProps(estimator, router))

  override def preStart = {
    super.preStart()
    val frequency = dealer.estimatedQueryFrequency * 2
    log.info("[AContentService] preStart")
    context.system.scheduler.schedule(frequency / 2, frequency, self, PullTick)
    context.system.scheduler.schedule(frequency, frequency, self, PushTick)
  }

  override def receive = manage(Nil, Nil)

  override def postStop = {
    super.postStop()
    log.info("[AContentService] postStop")
    context.stop(router)
    workers.foreach(w => context.stop(w))
  }

  def manage(consumers: List[ActorRef], estims: Estimations): Receive = {

    case PullSubjects => context.become(manage(sender :: consumers, estims))

    case PushingEstimations(chunk) => context.become(manage(consumers, chunk ::: estims))

    case PullTick => consumers match {
      case Nil => Unit
      case c::cs => dealer.pullSubjectsChunk.onComplete {
        case Success(Right(chunk)) => {
          c ! PulledSubjects(chunk)
          context.become(manage(cs, estims))
        }
        case Success(Left(err)) =>
          log.error(err)
        case Failure(ex) =>
          logWarning(ex)
      }
    }

    case PushTick => estims match {
      case Nil => Unit
      case e::es => dealer.pushEstimation(e).onComplete {
        case Success(Right(_)) => context.become(manage(consumers, es))
        case Success(Left(err)) => log.error(err)
        case Failure(ex) => logWarning(ex)
      }
    }

    case x => log.warning("Unexpected case type in content producer: " + x)
  }

  def logWarning(ex: Throwable) = log.warning(ex.getMessage)

}

object AContentService {
  /** Message used by the producer and assigned by manager for continuously pulling the
    * data-source, while in the pulling state. */
  case object PullTick
  /** Message used by the producer and assigned by manager for continuously pushing to the
    * data-source, while in the pushing state. */
  case object PushTick
  case class ProducingError(msg: ErrorMsg)

  def routerProps(producer: ActorRef) = Props(new AContentRouter(producer))
  def workerProps(e: TotalEstimator, router: ActorRef) =
    Props(new AContentEstimator(e, router))
}