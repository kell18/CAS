package tests

import cas.web.dealers.VkApiDealer
import cas.web.interface.{ImplicitActorSystem, InterfaceControl}
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._
import akka.util.Timeout
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class VkApiDealerSpec extends Specification {
  import ImplicitActorSystem._

  "VkApiDealer" should {
    val lentach = -29534144
    val dealer = new VkApiDealer(lentach, None)

    "Build request" in {
      "with 2 params" in {
        val correctReq = dealer.apiUrl + "test_1?t=1&r=qwe"
        correctReq mustEqual dealer.buildRequest("test_1", ("t", "1") ::("r", "qwe") :: Nil)
      }
      "with 1 param" in {
        val correctReq = dealer.apiUrl + "test_1?t=1"
        correctReq mustEqual dealer.buildRequest("test_1", ("t", "1") :: Nil)
      }
      "with 0 params" in {
        val correctReq = dealer.apiUrl + "test_1"
        correctReq mustEqual dealer.buildRequest("test_1", Nil)
        correctReq mustNotEqual dealer.buildRequest("test_2", Nil)
      }
    }

    "Estimate chunks lim" in {
      val postsToSift = dealer.siftCount.toLong
      val chLimF = dealer.estimateChunkLim
      val result = Await.ready(chLimF, Timeout(10.seconds))
      result.foreach(ch => {
        ch must beGreaterThan(0)
      })
    }
  }
}
