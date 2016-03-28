package cas.analysis

import java.util.Date

import cas.analysis.estimation._
import cas.analysis.subject.Subject
import cas.analysis.subject.components.{CreationDate, Likability, Virality}
import org.joda.time.DateTime
import org.specs2.mutable.Specification

class LoyaltyEstimatorSpec extends Specification {
  "LoyaltyEstimator" should {
    val estimator = new LoyaltyEstimator(LoyaltyConfigs(10.0, 1.0))
    "Return error message" in {
      "with no likes" in {
        val subj = Subject(List(CreationDate(DateTime.now()), Virality(10.0)))
        val actuality = estimator estimateActuality subj
        println("[LoyaltyEstimator] Left msg (no likes): " + actuality.left.get)
        actuality.right.getOrElse(-1.0) must beCloseTo(-1.0, 0.001)
      }

      "with no date" in {
        val subj = Subject(List(Likability(5.0), Virality(10.0)))
        val actuality = estimator estimateActuality subj
        println("[LoyaltyEstimator] Left msg (no date): " + actuality.left.get)
        actuality.right.getOrElse(-1.0) must beCloseTo(-1.0, 0.001)
      }
    }

    "Return actuality" in {
      "Without unnecessary components" in {
        val subj = Subject(List(Likability(5.0), CreationDate(DateTime.now())))
        val actuality = estimator estimateActuality subj
        actuality.right.getOrElse(-10.0) must beGreaterThanOrEqualTo(0.0)
      }
      "Gte 1.0" in {
        val subj = Subject(List(Likability(5.0), Virality(11.0), CreationDate(DateTime.now())))
        val actuality = estimator estimateActuality subj
        actuality.right.getOrElse(-10.0) must beGreaterThanOrEqualTo(1.0)
      }
      "Lt 1.0 (on bound)" in {
        val subj = Subject(List(Likability(9.9999), Virality(0.0), CreationDate(DateTime.now())))
        val actuality = estimator estimateActuality subj
        actuality.right.getOrElse(10.0) must beLessThan(1.0)
      }
      "Lt 1.0" in {
        val subj = Subject(List(Likability(5.0), Virality(0.0), CreationDate(DateTime.now())))
        val actuality = estimator estimateActuality subj
        actuality.right.getOrElse(10.0) must beLessThan(1.0)
      }
    }
  }
}
