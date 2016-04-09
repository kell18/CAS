package cas.analysis

import cas.analysis.estimation.{InverseRelevanceConfigs, InverseRelevanceEstimator, LoyaltyEstimator}
import cas.analysis.subject.Subject
import cas.analysis.subject.components.Description
import org.specs2.mutable.Specification
import cas.utils.StdImplicits.RightBiasedEither

/**
  * Created by a.bikeev on 08.04.2016.
  */
class InverseEstimatorSpec extends Specification {
  import cas.web.interface.ImplicitRuntime._
  "LoyaltyEstimator" should {
    val estimator = new InverseRelevanceEstimator(InverseRelevanceConfigs(0.5f))
    "return estimation" in {
      val subj = Subject(Description("parrot") :: Nil)
      estimator.estimateActuality(subj) must beRight
    }

    "return some estimation on relevant request" in {
      val subj = Subject(Description("parrot") :: Nil)
      estimator.estimateActuality(subj).get must beGreaterThan(0.3)
    }

    "return zero estimation on non-relevant request" in {
      val subj = Subject(Description("dfghjkltyut") :: Nil)
      estimator.estimateActuality(subj).get must beLessThan(0.0001)
    }
  }

}
