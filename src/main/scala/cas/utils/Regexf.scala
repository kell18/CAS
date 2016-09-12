package cas.utils

import java.util.regex.{Matcher, Pattern}

object Regexf {
  val wordsBound = "(\\b[^\\p{L}]+\\b)" // TODO: Care
  val allExceptText = "[^\\w\\s\\d\\p{L}]+"
  val upperCase = "[A-Z|\\p{Lu}]"
  val mainPunctuation = "[,\\.;!\"]+"

  def cleanAndSplit(str: String, byRegexp: String = wordsBound) = {
    val ptr = Pattern.compile(byRegexp)
    ptr.split(cleanToText(str))
  }

  def cleanToText(str: String) = {
    val sepsAndData: Pattern = Pattern.compile(allExceptText)
    val sepsAndDataM: Matcher = sepsAndData.matcher(str)
    sepsAndDataM.replaceAll("")
  }

  def countItems(str: String, regexp: String) = {
    val ptr = Pattern.compile(regexp)
    val matcher = ptr.matcher(str)
    var count = 0
    while (matcher.find) count += 1
    count
  }
}