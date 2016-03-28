package cas.analysis

import java.util.Date

import cas.analysis.estimation._
import cas.analysis.subject.Subject
import cas.analysis.subject.components.{CreationDate, Likability, Virality}
import org.joda.time.DateTime
import org.specs2.mutable.Specification

class TotalEstimatorSpec extends Specification {
  "TotalEstimator" should {
    "Return error" in {
      val estims = new LoyaltyEstimator(LoyaltyConfigs(10.0, 1.0, 0.3)) :: Nil
      val estimator = new TotalEstimator(estims)

      "With lack of subject components" in {
        val subj = new Subject(List(CreationDate(DateTime.now())))
        val actuality = estimator estimateActuality subj
        println("[TotalEstimator] Left msg (lack of components): " + actuality.left.get)
        actuality.right.getOrElse(-1.0) must beCloseTo(-1.0, 0.001)
      }
    }

    "Return actuality" in {
      "With weight" in {
        val estims = new LoyaltyEstimator(LoyaltyConfigs(10.0, 1.0, 0.3)) :: Nil
        val estimator = new TotalEstimator(estims)
        val subj = new Subject(List(Likability(5.0), Virality(11.0), CreationDate(DateTime.now())))
        val actuality = estimator estimateActuality subj
        actuality.right.getOrElse(-10.0) must beGreaterThanOrEqualTo(0.3)
      }
    }
  }
}
