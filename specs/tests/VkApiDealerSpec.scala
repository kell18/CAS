package tests

import cas.analysis.subject.components.ID
import cas.subject.Subject
import cas.web.dealers.VkApiDealer
import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scala.concurrent.Await

class VkApiDealerSpec extends Specification {
  import cas.web.interface.ImplicitActorSystem._
  val zeroID = new ID("NO ID TEST")

  "VkApiDealer" should {
    val lentach = -29534144
    val dealer = new VkApiDealer(lentach, None)

    // TODO: Move in sep testSpec
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

    "Pull access_token" in {
      val token = Await.result(dealer.tokenF, Duration("10 seconds"))
      println("[VkApiDealerSpec] acces token: " + token)
      "Length gt zero" in {
        token.length must be greaterThan 0
      }
    }

    "Pull subjects chunk" >> {
      val subjects = Await.result(dealer.pullSubjectsChunk, Duration("10 seconds"))

      "With no zero length" in {
        subjects.length must be greaterThan 0
      }

      "With essential components" in {
        val obj = subjects.head.getComponent[Subject].right.toOption
        obj must beSome

        subjects.head.getComponent[ID].right.getOrElse(zeroID) mustNotEqual zeroID
        obj.get.getComponent[ID].right.getOrElse(zeroID) mustNotEqual zeroID
      }
    }
  }
}
