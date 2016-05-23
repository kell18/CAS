package cas.utils

import cas.analysis.subject.components.{CreationDate, Likability, Virality}
import org.specs2.mutable.Specification

class UtilsSpec extends Specification {

  "Utils" should {
    "Escape json" in {
      val unescaped =
        """{"test": "123,\ http://test.com", "123": "http://www.rbc.ru/society/19/05/2016/573db86e9a794732a702a902"}"""
      val escaped =
        """{\"test\": \"123,\\ http:\/\/test.com\", \"123\": \"http:\/\/www.rbc.ru\/society\/19\/05\/2016\/573db86e9a794732a702a902\"}"""
      Utils.escapeJson(unescaped) must beEqualTo(escaped)
    }
  }
}