package cas.utils

import org.specs2.mutable.Specification

class RegexfSpec extends Specification  {

  "Regexf" should {
    val testString = "Привет Вася123, hello world../. #$%^& \uD83D\uDE03 \uD83D\uDE03"
    val oneWord = "oneWord123"

    "clean to text" in {
      val cleanStr = Regexf.cleanToText(testString)
      cleanStr must contain(" ")
      cleanStr must not contain ","
      cleanStr must not contain "../."
      cleanStr must not contain "#$%^&"
      cleanStr must not contain "\uD83D\uDE03 \uD83D\uDE03"
    }

    "split string to words" in {
      val words = Regexf.cleanAndSplit(testString)
      words.length must beEqualTo(4)
    }

    "split one word to one word" in {
      val word = Regexf.cleanAndSplit(oneWord)
      word.length must beEqualTo(1)
    }

    "count items" in {
      val testStr = "Hello, ВАСЯН! фыва))) \" ; ... -- ++ \uD83D\uDE03 \uD83D\uDE03"
      Regexf.countItems(testStr, Regexf.allExceptText) must beEqualTo(10)
      Regexf.countItems(testStr, Regexf.upperCase) must beEqualTo(6)
      Regexf.countItems(testStr, Regexf.mainPunctuation) must beEqualTo(5)
    }
  }
}
