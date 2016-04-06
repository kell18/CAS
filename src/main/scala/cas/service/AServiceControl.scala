package cas.service

import cas.analysis.estimation.{LoyaltyConfigs, LoyaltyEstimator, TotalEstimator}
import cas.web.dealers.DealersFactory
import cas.web.interface.ImplicitActorSystem
import cas.web.model.UsingDealer
import org.joda.time.Period
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import cas.service.AProducer.QueryTick
import cas.service.AServiceControl.ServiceStatus.ServiceStatus

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object AServiceControl {
  /** Stop service if any active and init new one */
  case class  Start(dealerID: UsingDealer)
  /** Stop service if any active but don't stop itself */
  object      Stop

  object      GetStatus
  case class  Status(status: ServiceStatus)

  object ServiceStatus extends Enumeration {
    type ServiceStatus = Value
    val Active = Value("Active")
    val Inactive = Value("Inactive")
    val Paused = Value("Paused")
  }

  private[cas] case class Init(dealer: ContentDealer)

  private def producerProps(dealer: ContentDealer, estimator: TotalEstimator) = Props(new AProducer(dealer, estimator))
  private def routerProps(producer: ActorRef) = Props(new ARouter(producer))
  private def workerProps(estimator: TotalEstimator, router: ActorRef) = Props(new AWorkerEstimator(estimator, router))
}

class AServiceControl extends Actor with ActorLogging {
  import AServiceControl._
  import ImplicitActorSystem._
  import system.dispatcher

  val workersCount = 2 // Runtime.getRuntime.availableProcessors
  val estimator = new TotalEstimator(new LoyaltyEstimator(LoyaltyConfigs(Map(
    new Period().plusMinutes(5) ->  5.0,
    new Period().plusMinutes(10) -> 10.0,
    new Period().plusMinutes(15) -> 15.0,
    new Period().plusMinutes(20) -> 20.0
  ))) :: Nil)

  var querySchedule: Option[Cancellable] = None

  //(new Period().plusMinutes(23)).toStandardSeconds.getSeconds

  override def receive: Receive = serve(None, None, Nil)

  def serve(producer: Option[ActorRef], router: Option[ActorRef], workers: List[ActorRef]): Receive = {
    case Start(dealerConfigs) => {
      self ! Stop
      DealersFactory.buildDealer(dealerConfigs.id) match {
        case Success(dealer) => self ! Init(dealer)
        case Failure(NonFatal(e)) => log.error(s"[AServiceControl] Cannot start content service: `${e.getMessage}`")
      }
    }

    case Stop => {
      workers.foreach(context.stop)
      router.foreach(system.stop)
      producer.foreach(system.stop)
      // querySchedule.foreach(q => if (!q.isCancelled) q.cancel)
      log.info("[AServiceControl] Service successfully stopped.")
      context.become(serve(None, None, Nil))
    }

    case GetStatus => if (producer.isDefined && router.isDefined && workers.nonEmpty) sender ! Status(ServiceStatus.Active)
                      else sender ! Status(ServiceStatus.Inactive)

    case Init(dealer) => {
      val prod = Some(system.actorOf(producerProps(dealer, estimator), "Producer"))
      val frequency = dealer.estimatedQueryFrequency
      querySchedule = Some(context.system.scheduler.schedule(frequency, frequency, prod.get, QueryTick))
      val router = Some(system.actorOf(routerProps(prod.get), "Router"))
      val workers = for (i <- 1 to workersCount)
        yield context.actorOf(workerProps(estimator, router.get), "Worker-"+i)
      log.info("[AServiceControl] Service successfully started.")
      context.become(serve(prod, router, workers.toList))
    }
  }
}
