package cas.utils

import cas.analysis.subject.components.{CreationDate, Likability, Virality}
import org.specs2.mutable.Specification

class WebSpec extends Specification {

  "Build request" in {
    val url = "https://api.vk.com/method/"
    "with 2 params" in {
      val page = "wall.get"
      val correctReq = url + page + "?t=1&r=qwe"
      correctReq mustEqual Web.buildRequest(url)(page)(("t", "1") ::("r", "qwe") :: Nil)
    }
    "with 1 param" in {
      val page = "test_1"
      val correctReq = url + page + "?t=1"
      correctReq mustEqual Web.buildRequest(url)(page)(("t", "1") :: Nil)
    }
    "with 0 params" in {
      val page = "test_1"
      val correctReq = url + page
      correctReq mustEqual Web.buildRequest(url)(page)(Nil)
      correctReq mustNotEqual Web.buildRequest(url)("test_2")(Nil)
    }
  }
}
