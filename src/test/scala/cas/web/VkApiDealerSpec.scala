package cas.web

import cas.analysis.subject.components.ID
import cas.analysis.subject.Subject
import cas.persistence.ElasticSpec._
import cas.persistence.searching.ElasticSearch
import cas.service.ARouter.Estimation
import cas.service.ContentDealer
import cas.web.dealers.DealersFactory
import cas.web.dealers.vk.VkApiDealer
import cas.web.dealers.vk.VkApiProtocol.VkApiConfigs
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import utils.AkkaToSpec2Scope
import scala.concurrent.Await
import scala.concurrent.duration._

class VkApiDealerSpec extends Specification with NoTimeConversions {
  sequential // forces all tests to be run sequentially
  val lentachID = -29534144L
  val zeroID = new ID("TEST_ID_!@#$%")

  "VkApiDealer" should  {
    "constructable with DealersFactory" in new AkkaToSpec2Scope {
      val dealer = DealersFactory.buildDealer(VkApiDealer.id, createTestingElasticS())
      dealer must beSuccessfulTry[ContentDealer]
    }

    "gt zero estimated query frequency" in new AkkaToSpec2Scope {
      val dealer = DealersFactory.buildDealer(VkApiDealer.id, createTestingElasticS())
      dealer must beSuccessfulTry
      dealer.get.estimatedQueryFrequency must beGreaterThan(0.seconds)
    }

    "pull subjects chunk with essential components"  in new AkkaToSpec2Scope {
      val dealer = DealersFactory.buildDealer(VkApiDealer.id, createTestingElasticS())
      dealer must beSuccessfulTry
      val subjects = Await.result(dealer.get.pullSubjectsChunk, 20.seconds).right.get
      subjects.length must be greaterThan 0

      val obj = subjects.head.getComponent[Subject].right.toOption
      obj must beSome

      subjects.head.getComponent[ID].right.getOrElse(zeroID) mustNotEqual zeroID
      obj.get.getComponent[ID].right.getOrElse(zeroID) mustNotEqual zeroID
    }

    /*"push estimation without ID return Left" in new AkkaToSpec2Scope {
      val dealer = DealersFactory.buildDealer(VkApiDealer.id, createTestingElasticS())
      dealer must beSuccessfulTry
      val fake = new Estimation(Subject(List()), 0.0)
      val response = Await.result(dealer.get.pushEstimations(fake :: Nil), 10.seconds)
      response must beLeft
    }*/
  }
}
