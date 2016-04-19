package cas.analysis

import cas.analysis.estimation.{InvRelevanceConfigs, InvRelevanceEstimator, LoyaltyEstimator}
import cas.analysis.subject.Subject
import cas.analysis.subject.components.Description
import cas.persistence.searching.ElasticSearch
import org.specs2.mutable.Specification
import cas.utils.StdImplicits.RightBiasedEither
import org.specs2.time.NoTimeConversions
import utils.AkkaToSpec2Scope
import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by a.bikeev on 08.04.2016.
  */
class InverseEstimatorSpec extends Specification with NoTimeConversions {
  import cas.web.interface.ImplicitRuntime._
  import cas.persistence.ElasticSpec.createTestingElasticS
  sequential
  "LoyaltyEstimator" should {
    sequential
    "return estimation" in new AkkaToSpec2Scope {
      val searcher = createTestingElasticS()
      val initResult = Await.result(searcher.initStorage, 10.seconds)
      Thread.sleep(1100)
      val estimator = new InvRelevanceEstimator(InvRelevanceConfigs(searcher, 0.5))
      val subj = Subject(Description("parrot") :: Nil)
      val errOrActuality = estimator.estimateActuality(subj)
      errOrActuality must beRight
    }

    "return some estimation on relevant request" in new AkkaToSpec2Scope {
      val id = "inverseestimatorspectestid"
      val searcher = createTestingElasticS()
      val pushResult = searcher.pushEntity(id, "parrot")
      Await.result(pushResult, 10.seconds) must beRight
      val estimator = new InvRelevanceEstimator(InvRelevanceConfigs(searcher, 0.001))
      val subj = Subject(Description("parrot") :: Nil)
      val errOrActuality = estimator.estimateActuality(subj)
      errOrActuality must beRight
      errOrActuality.get must beGreaterThan(0.3)
    }

    "return zero estimation on non-relevant request" in new AkkaToSpec2Scope {
      val estimator = new InvRelevanceEstimator(InvRelevanceConfigs(createTestingElasticS(), 0.5))
      val subj = Subject(Description("dfghjkltyut") :: Nil)
      val errOrActuality = estimator.estimateActuality(subj)
      errOrActuality must beRight
      errOrActuality.get must beLessThan(0.0001)
    }

    "clean up after tests" in new AkkaToSpec2Scope {
      val searcher = createTestingElasticS()
      val disposeResult = Await.result(searcher.disposeStorage, 10.seconds)
      disposeResult must beRight
    }
  }

}
