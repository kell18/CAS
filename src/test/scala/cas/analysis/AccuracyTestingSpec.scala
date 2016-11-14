package cas.persistence

import akka.actor.ActorSystem
import cas.analysis.subject.Subject
import cas.analysis.subject.components._
import cas.analysis.estimation.{CorrectnessConfigs, CorrectnessEstimator}
import cas.persistence.searching.ElasticSearch
import cas.service.ARouter.Estimation
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import utils.AkkaToSpec2Scope

import scala.concurrent.Await
import scala.concurrent.duration._
import cas.utils.StdImplicits.RightBiasedEither
import org.joda.time.DateTime

class AccuracyTestingSpec extends Specification {
  import ElasticSpec._
  sequential

  case class Comment(txt: String, passedSecs: Int, likes: Int = 0, isDel: Boolean = false)
  case class Estim(est: Estimation, isDelEstim: Boolean)

  /*val realEstimsP1 = Estimation(Subject(List(Description(""), CreationDate(new DateTime().minusMinutes()), Likability(0))), 1.0) ::
    Estimation(Subject(List(Description(""), CreationDate(new DateTime().minusMinutes()), Likability(0))), 1.0) ::
    Estimation(Subject(List(Description(""), CreationDate(new DateTime().minusMinutes()), Likability(0))), 1.0) :: Nil*/

  /*def computeAccuracy(estims: List[Estim]) = (for {
    e <- estims
  } yield if (e.isDelEstim != e.cmt.isDel) 1 else 0).sum*/

  /*"AccuracyTests" should {
    sequential

    "simple correctness" in new AkkaToSpec2Scope {
      // val correctnesEst = new CorrectnessEstimator(CorrectnessConfigs())
      // val actualityVals = realEstimsP1.map(e => correctnesEst.estimateActuality(e.subj))
      // ...
    }

    "simple correctness" in new AkkaToSpec2Scope {
      val elastic = createTestingElasticS()
      Await.result(elastic.initStorage, 10.seconds) must beRight

    }

    "compute scores" in {
      "for exact match" in new AkkaToSpec2Scope {
        val elastic = createTestingElasticS()
        Await.result(elastic.initStorage, 10.seconds) must beRight
        val searchResp = Await.result(elastic.queryEntityScore(defaultEntity), 10.seconds)
        searchResp must beRight
        searchResp.get.maxScore must beSome
        searchResp.get.maxScore.get must beGreaterThan(0.1)
      }
      "for partial match" in new AkkaToSpec2Scope {
        val elastic = createTestingElasticS()
        Await.result(elastic.initStorage, 10.seconds) must beRight
        val searchResp = Await.result(elastic.queryEntityScore(defaultEntity.split(" ")(0)), 10.seconds)
        searchResp must beRight
        searchResp.get.maxScore must beSome
        searchResp.get.maxScore.get must beGreaterThan(0.1) and beLessThan(0.8)
      }
      "for none match" in new AkkaToSpec2Scope {
        val elastic = createTestingElasticS()
        Await.result(elastic.initStorage, 10.seconds) must beRight
        val searchResp = Await.result(elastic.queryEntityScore("qwertyuiop$%^&*"), 10.seconds)
        searchResp must beRight
        searchResp.get.maxScore must beNone
      }
    }

    /*"delete documents on expire" in new AkkaToSpec2Scope {
      Thread.sleep(60000)
      val elastic = new ElasticSearch("http://localhost:9201", index, mtype, ttl)
      Await.result(elastic.initStorage, 10.seconds) must beRight
      val delResp =  Await.result(elastic.getEntity(id), 10.seconds)
      delResp must beRight
      delResp.get must beNone
    }*/

    "dispose storage" in new AkkaToSpec2Scope {
      val elastic = createTestingElasticS()
      Await.result(elastic.disposeStorage, 10.seconds) must beRight
    }
  }*/

}
