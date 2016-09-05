package cas.analysis

import java.util.Date
import cas.analysis.estimation._
import cas.utils.StdImplicits.RightBiasedEither
import cas.analysis.subject.Subject
import cas.analysis.subject.components.{CreationDate, Likability, Virality}
import org.joda.time.{DateTime, Period, Duration}
import org.specs2.mutable.Specification

class LoyaltyEstimatorSpec extends Specification {
  "LoyaltyEstimator" should {

    val estimator = new LoyaltyEstimator(LoyaltyConfigs(Map(
      Duration.standardMinutes(5) ->  0.5,
      Duration.standardMinutes(10) -> 0.2,
      Duration.standardMinutes(15) -> 0.142857143,
      Duration.standardMinutes(20) -> 0.1
    )))
    val basePeriod = new DateTime().minusMinutes(3)
    val firstPeriod = new DateTime().minusMinutes(6)
    val thirdPeriod = new DateTime().minusMinutes(16)
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
        "base period" in {
          val subj = Subject(List(Likability(0.0), CreationDate(basePeriod)))
          val actuality = estimator estimateActuality subj
          actuality.get must beCloseTo(1.0, 0.0001)
        }
        "first" in {
          val likes = 2.0
          val subj = Subject(List(Likability(likes), CreationDate(firstPeriod)))
          val actuality = estimator estimateActuality subj
          actuality.get must beCloseTo(0.5 * likes, 0.0001)
        }

        "second" in {
          val likes = 6.0
          val subj = Subject(List(Likability(likes), CreationDate(thirdPeriod)))
          val actuality = estimator estimateActuality subj
          actuality.get must beCloseTo(0.142857143 * likes, 0.0001)
        }
      }
    }
  }
}
