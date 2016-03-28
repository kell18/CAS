package cas.service

import java.util.Date

import akka.actor.Props
import cas.analysis.subject.components.ID
import cas.analysis.estimation._
import cas.analysis.subject.Subject
import cas.analysis.subject.components.{CreationDate, Likability}
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import utils.AkkaToSpec2Scope

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

class ContentServiceSpec extends Specification with NoTimeConversions {
  sequential // forces all tests to be run sequentially

  "ServiceSpecs" should {

    "Push same subjects as received in pullSubjectsChunk" in new AkkaToSpec2Scope {
      val estimator = new TotalEstimator(new LoyaltyEstimator(LoyaltyConfigs(6.0, 0.0)) :: Nil)
      val dealer = new ContentDealer {
        val pullingSubjects = Subject(ID("ID1") :: Likability(10.0) :: CreationDate(DateTime.now()) ::
          Subject(ID("ID2") :: Likability(5.0) :: Nil) :: Nil) :: Nil
        var pushedEstimations = List[Estimation]()
        var isPulled = false
        var isPushed = false

        override def estimatedQueryFrequency = 1.0.second

        override def pullSubjectsChunk: Future[Either[String, List[Subject]]] = {
          if (isPulled) Future { throw new Exception("Pull only once for testing") }
          else {
            isPulled = true
            Future { Right(pullingSubjects) }
          }
        }

        override def pushEstimation(estim: Estimation): Future[Either[String, Any]] = {
          pushedEstimations = estim :: pushedEstimations
          isPushed = true
          Future { Right(true) }
        }
      }

      def waitForPushF = Future {
        while(!dealer.isPushed) {}
        dealer.pushedEstimations
      }

      val service = system.actorOf(Props(new AContentService(dealer, estimator)))
      val subjects = Await.result(waitForPushF, Duration("10 seconds")).flatten(e => e.subj :: Nil)
      system.stop(service)
      subjects must containTheSameElementsAs(dealer.pullingSubjects)
    }
  }
}
