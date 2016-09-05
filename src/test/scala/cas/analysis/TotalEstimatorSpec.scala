package cas.analysis

import java.util.Date

import cas.analysis.estimation._
import cas.analysis.subject.Subject
import cas.analysis.subject.components.{CreationDate, Likability, Virality}
import org.joda.time.{DateTime, Duration, Period}
import org.specs2.mutable.Specification

class TotalEstimatorSpec extends Specification {
  "TotalEstimator" should {
    val estims = new LoyaltyEstimator(LoyaltyConfigs(Map(
      Duration.standardMinutes(2) -> 2.0)
    )) :: Nil
    val estimator = new TotalEstimator(estims)

    "Return error" in {
      "With lack of subject components" in {
        val subj = Subject(List(CreationDate(DateTime.now())))
        val actuality = estimator estimateActuality subj
        println("[TotalEstimator] Left msg (lack of components): " + actuality.left.get)
        actuality.right.getOrElse(-1.0) must beCloseTo(-1.0, 0.001)
      }
    }

    "Return actuality" in {
      "With weight" in {
        val subj = Subject(List(Likability(5.0), Virality(11.0), CreationDate(DateTime.now())))
        val actuality = estimator estimateActuality subj
        actuality.right.getOrElse(-10.0) must beGreaterThanOrEqualTo(0.3)
      }
    }
  }
}
