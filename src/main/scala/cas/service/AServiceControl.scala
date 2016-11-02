package cas.service

import cas.analysis.estimation.{ActualityEstimator, LoyaltyConfigs, LoyaltyEstimator, TotalEstimator}
import cas.web.dealers.DealersFactory
import cas.web.interface.ImplicitRuntime
import cas.web.model.UsingDealer
import org.joda.time.Period
import akka.pattern.ask
import akka.pattern._
import akka.util.Timeout
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import cas.service.AProducer.QueryTick
import cas.service.AServiceControl.ServiceStatus.ServiceStatus

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object AServiceControl {
  /** Stop service if any active and init new one */
  case class  Start(dealerID: ContentDealer, estim: ActualityEstimator)
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

  private def producerProps(dealer: ContentDealer) = Props(new AProducer(dealer))
  private def routerProps(producer: ActorRef) = Props(new ARouter(producer))
  private def workerProps(estimator: ActualityEstimator, router: ActorRef) = Props(new AWorkerEstimator(estimator, router))
}

class AServiceControl extends Actor with ActorLogging {
  import AServiceControl._
  import ImplicitRuntime._
  import system.dispatcher
  implicit val timeout = Timeout(3.seconds)

  val workersCount = 2 // Runtime.getRuntime.availableProcessors
  var querySchedule: Option[Cancellable] = None
  var extraId = 0 // For resolving actors names collisions

  override def receive: Receive = serve(None, None, Nil)

  def serve(producer: Option[ActorRef], router: Option[ActorRef], workers: List[ActorRef]): Receive = {
    case Start(dealer, estimator) => {
      deactivate(producer, router, workers)
      activate(dealer, estimator)
      sender ! Status(ServiceStatus.Active)
    }

    case Stop => {
      deactivate(producer, router, workers)
      log.info("[AServiceControl] Service successfully stopped.")
      sender ! Status(ServiceStatus.Inactive)
      context.become(serve(None, None, Nil))
    }

    case GetStatus => {
      if (producer.isDefined && router.isDefined && workers.nonEmpty) sender ! Status(ServiceStatus.Active)
      else sender ! Status(ServiceStatus.Inactive)
    }

    case unexpected => log.error(s"[AServiceControl] Unexpected case type $unexpected");
  }

  // TODO: Graceful stop
  def deactivate(producer: Option[ActorRef], router: Option[ActorRef], workers: List[ActorRef]) = {
    workers.foreach(context.stop)
    router.foreach(context.stop)
    producer.foreach(context.stop)
    querySchedule.foreach(q => if (!q.isCancelled) q.cancel)
  }

  def activate(dealer: ContentDealer, estim: ActualityEstimator) = {
    extraId += 1
    val prod = Some(context.actorOf(producerProps(dealer), "Producer-e" + extraId))
    val tryInitDealer = Try(Await.result(dealer.initialize, 60.seconds))
    if (tryInitDealer.isSuccess) {
      val frequency = dealer.estimatedQueryFrequency
      querySchedule = Some(context.system.scheduler.schedule(frequency, frequency, prod.get, QueryTick))
      context.system.scheduler.maxFrequency
      val rout = Some(context.actorOf(routerProps(prod.get), "Router-e" + extraId))
      val wrkrs = for (i <- 1 to workersCount)
        yield context.actorOf(workerProps(estim, rout.get), s"Worker-e$extraId-$i")
      log.info("[AServiceControl] Service successfully started.")
      context.become(serve(prod, rout, wrkrs.toList))
    }
    else log.error(s"Cannot activate service, dealer initialization failed: `${tryInitDealer.failed.get.getMessage}`")
  }
}
