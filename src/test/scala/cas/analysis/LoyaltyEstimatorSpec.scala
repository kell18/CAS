package cas.analysis

import java.util.Date
import cas.analysis.estimation._
import cas.utils.StdImplicits.RightBiasedEither
import cas.analysis.subject.Subject
import cas.analysis.subject.components.{CreationDate, Likability, Virality}
import org.joda.time.{DateTime, Period}
import org.specs2.mutable.Specification

class LoyaltyEstimatorSpec extends Specification {
  "LoyaltyEstimator" should {
    val estimator = new LoyaltyEstimator(LoyaltyConfigs(Map(
      new Period().withSeconds(2) -> 2.0,
      new Period().withSeconds(5) -> 10.0)
    ))
    val firstPeriod = new DateTime().minusSeconds(3)
    val secondPeriod = new DateTime().minusSeconds(5)
    "Return error message" in {
      "with no likes" in {
        val subj = Subject(List(CreationDate(DateTime.now()), Virality(10.0)))
        val actuality = estimator estimateActuality subj
        println("[LoyaltyEstimator] Left msg (no likes): " + actuality.left.get)
        actuality.getOrElse(-1.0) must beCloseTo(-1.0, 0.001)
      }

      "with no date" in {
        val subj = Subject(List(Likability(5.0), Virality(10.0)))
        val actuality = estimator estimateActuality subj
        println("[LoyaltyEstimator] Left msg (no date): " + actuality.left.get)
        actuality.getOrElse(-1.0) must beCloseTo(-1.0, 0.001)
      }
    }

    "Return actuality" in {
      "without unnecessary components" in {
        val subj = Subject(List(Likability(5.0), CreationDate(DateTime.now())))
        val actuality = estimator estimateActuality subj
        actuality.get must beGreaterThanOrEqualTo(0.0)
      }
      "gte 1.0 with huge amount of likes" in {
        val subj = Subject(List(Likability(50.0), Virality(11.0), CreationDate(DateTime.now())))
        val actuality = estimator estimateActuality subj
        actuality.get must beGreaterThanOrEqualTo(1.0)
      }

      "by periods" in {
        "not done in time" in {
          val subj = Subject(List(Likability(1.0), CreationDate(firstPeriod)))
          val actuality = estimator estimateActuality subj
          actuality.get must beLessThan(1.0)
        }
        "just in time" in {
          val subj = Subject(List(Likability(2.0), CreationDate(firstPeriod)))
          val actuality = estimator estimateActuality subj
          actuality.get must beGreaterThan(0.0)
        }
        "after time" in {
          val subj = Subject(List(Likability(11.0), CreationDate(secondPeriod)))
          val actuality = estimator estimateActuality subj
          actuality.get must beGreaterThan(0.0)
        }
      }
    }
  }
}
