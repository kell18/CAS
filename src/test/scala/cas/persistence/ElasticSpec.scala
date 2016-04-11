package cas.persistence

import cas.persistence.searching.ElasticSearch
import cas.utils.Mathf
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import utils.AkkaToSpec2Scope

import scala.concurrent.Await
import scala.concurrent.duration._
import cas.utils.StdImplicits.RightBiasedEither

class ElasticSpec extends Specification with NoTimeConversions {
  sequential

  "Elasticsearch" should {
    sequential
    val index = "cas-testing-index"
    val mtype = "test"
    val id = "testid"
    val defaultEntity = "Red fox"
    "indexing documents" in new AkkaToSpec2Scope {
      val elastic = new ElasticSearch("http://localhost:9201", index, mtype)
      Await.result(elastic.initStorage, 10.seconds) must beRight
      elastic.pushIndexedEntity(id, defaultEntity)
      Thread.sleep(2000)
      val entity = Await.result(elastic.getEntity(id), 10.seconds)
      entity must beRight
      entity.get must beSome
      entity.get.get must beEqualTo(defaultEntity)
    }

    "compute scores" in {
      "for exact match" in new AkkaToSpec2Scope {
        val elastic = new ElasticSearch("http://localhost:9201", index, mtype)
        Await.result(elastic.initStorage, 10.seconds) must beRight
        val searchResp = Await.result(elastic.queryEntity(defaultEntity), 10.seconds)
        searchResp must beRight
        searchResp.get.maxScore must beSome
        searchResp.get.maxScore.get must beGreaterThan(0.1)
      }
      "for partial match" in new AkkaToSpec2Scope {
        val elastic = new ElasticSearch("http://localhost:9201", index, mtype)
        Await.result(elastic.initStorage, 10.seconds) must beRight
        val searchResp = Await.result(elastic.queryEntity(defaultEntity.split(" ")(0)), 10.seconds)
        searchResp must beRight
        searchResp.get.maxScore must beSome
        searchResp.get.maxScore.get must beGreaterThan(0.1) and beLessThan(0.8)
      }
      "for none match" in new AkkaToSpec2Scope {
        val elastic = new ElasticSearch("http://localhost:9201", index, mtype)
        Await.result(elastic.initStorage, 10.seconds) must beRight
        val searchResp = Await.result(elastic.queryEntity("qwertyuiop$%^&*"), 10.seconds)
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
      val elastic = new ElasticSearch("http://localhost:9201", index, mtype)
      Await.result(elastic.disposeStorage, 10.seconds) must beRight
    }
  }

}
