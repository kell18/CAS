package cas.persistence

import akka.actor.ActorSystem
import cas.math.Mathf
import cas.persistence.searching.ElasticSearch
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import utils.AkkaToSpec2Scope

import scala.concurrent.Await
import scala.concurrent.duration._
import cas.utils.StdImplicits.RightBiasedEither

object ElasticSpec {
  val host = "http://localhost:9201"
  val index = "cas-testing-index"
  val mtype = "test"
  val id = "testid"

  def createTestingElasticS(ind: String = index, tp: String = mtype)
                           (implicit system: ActorSystem) = {
    new ElasticSearch(host, ind, tp)
  }
}

class ElasticSpec extends Specification with NoTimeConversions {
  import ElasticSpec._
  sequential

  "Elasticsearch" should {
    sequential

    val defaultEntity = "Red fox"
    "indexing documents" in new AkkaToSpec2Scope {
      val elastic = createTestingElasticS()
      Await.result(elastic.initStorage, 10.seconds) must beRight
      elastic.pushEntity(id, defaultEntity)
      Thread.sleep(2000)
      val entity = Await.result(elastic.getEntity(id), 10.seconds)
      entity must beRight
      entity.get must beSome
      entity.get.get must beEqualTo(defaultEntity)
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
  }

}
