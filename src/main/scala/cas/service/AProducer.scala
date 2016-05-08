package cas.service

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import cas.analysis.estimation._
import cas.analysis.subject.components.Description
import cas.utils.UtilAliases._
import cas.utils.StdImplicits.RightBiasedEither

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object AProducer {
  case object QueryTick
  case class ProducingError(msg: ErrorMsg)
}

/** Manages content flow btw dealer and router
  *
  * @param dealer concrete realization of ContentDealer
  */
class AProducer(dealer: ContentDealer) extends Actor with ActorLogging { // TODO
  import AProducer._
  import ARouter._
  import cas.web.interface.ImplicitRuntime._
  import system.dispatcher

  val timeout = 10.seconds // TODO: Make global

  override def preStart = {
    super.preStart()
    log.info("[AProducer] preStart")
  }

  override def receive = serve(Nil, Nil)

  override def postStop = {
    super.postStop()
    log.info("postStop")
  }

  def serve(consumers: List[ActorRef], estimChunks: List[Estimations]): Receive = {

    case PullSubjects => changeContext(sender :: consumers, estimChunks)

    case PushingEstimations(chunk) => changeContext(consumers, chunk :: estimChunks)

    case QueryTick => {
      val estims = estimChunks.flatten // TODO: Send one by one chunk
      if (estims.nonEmpty) Try(Await.result(dealer.pushEstimations(estims), timeout)) match {
        case Success(Right(_)) => changeContext(consumers, Nil)
        case Success(Left(err)) =>
          log.error(s"Dealer returns Left on pushEstims: `$err`")
          changeContext(consumers, Nil)
        case Failure(NonFatal(ex)) =>
          log.warning(s"Dealer returns error on pushEstims: `${ex.getMessage}`")
          changeContext(consumers, Nil)
      }
      else consumers match {
        case Nil => Unit
        case c::cs => Try(Await.result(dealer.pullSubjectsChunk, timeout)) match {
          case Success(Right(chunk)) =>
            c ! PulledSubjects(chunk)
            changeContext(cs, estimChunks)
          case Success(Left(err)) => log.error(s"Dealer returns Left on pullChunks: `$err`")
          case Failure(NonFatal(ex)) => log.warning(s"Dealer returns error on pullChunks: " +
            s"`${ex.getLocalizedMessage}` with stacktrace: `${ex.getStackTrace.mkString(", ")}`")  // TODO: Rm
        }
      }
    }

    case x => log.warning(s"Unexpected case type in content producer: $x")
  }

  def changeContext(consumers: List[ActorRef], estims: List[Estimations]) = {
    if (estims.flatten.length > 25) {
      log.warning("estims length: " + estims.flatten.length)
    }
    context.become(serve(consumers, estims))
  }

}