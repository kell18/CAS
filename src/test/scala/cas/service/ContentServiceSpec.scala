package cas.service

import java.util.Date

import akka.actor.Props
import cas.analysis.subject.components.ID
import cas.analysis.estimation._
import cas.analysis.subject.Subject
import cas.analysis.subject.components.{CreationDate, Likability}
import cas.service.ARouter.Estimation
import cas.service.AServiceControl.{Start, Stop}
import cas.utils.UtilAliases.Estimations
import org.joda.time.{DateTime, Period}
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import utils.AkkaToSpec2Scope

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

class ContentServiceSpec extends Specification with NoTimeConversions {
  sequential // forces all tests to be run sequentially

  "ServiceSpecs" should {

    "Push proper estims and subjs as received in pullSubjectsChunk" in new AkkaToSpec2Scope {
      val estimator = new TotalEstimator(new LoyaltyEstimator(LoyaltyConfigs(Map(
        org.joda.time.Duration.standardMinutes(1) ->  0.3
      ))) :: Nil)
      val likesCnt = 20.0

      val dealer = new ContentDealer {
        val pullingSubjects = Subject(ID("ID1") :: Likability(likesCnt) :: CreationDate(DateTime.now().minusMinutes(2)) ::
          Subject(ID("ID2") :: Nil) :: Nil) :: Nil
        var pushedEstimations = List[Estimation]()
        var isPulled = false
        var isPushed = false

        override def estimatedQueryFrequency = 1.0.second

        override def pullSubjectsChunk: Future[Either[String, List[Subject]]] = {
          if (isPulled) Future { throw new Exception("Pull only once for testing") }
          else {
            isPulled = true
            Future { Thread.sleep(70L); Right(pullingSubjects) }
          }
        }

        override def pushEstimations(es: Estimations) = {
          pushedEstimations = es ::: pushedEstimations
          isPushed = true
          Future { Right(true) }
        }

        override def initialize = Future { }
      }

      def waitForPushF = Future {
        while(!dealer.isPushed) {}
        dealer.pushedEstimations
      }

      val service = system.actorOf(Props(new AServiceControl))
      service ! Start(dealer, estimator)
      val estims = Await.result(waitForPushF, Duration("10 seconds"))
      service ! Stop
      system.stop(service)

      val subjs = estims.flatten(e => e.subj :: Nil)
      subjs must containTheSameElementsAs(dealer.pullingSubjects)
      estims.head.actuality must beCloseTo(0.3 * likesCnt, 0.001)
    }
  }
}
