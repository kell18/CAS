package cas.utils

import java.io._

import cas.analysis.subject.Subject
import cas.service.ARouter.Estimation
import com.typesafe.config._
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.joda.time.DateTimeZone
import scala.xml.{Node, XML}

object Utils {
  val dataPath = "resources/data"
  val confPath = "resources/conf"
  val webModelPath = dataPath + "/web.model"

  val timeZone = DateTimeZone.forOffsetHours(+4)

  val configs = ConfigFactory.load(
    ConfigFactory.parseFile(new File(confPath + "/application.conf"))
  )

  // TODO: Move to Files
  def writeToFile(path: String, s: String): Unit = {
    val pw = new java.io.PrintWriter(new File(path))
    try { pw.write(s) } catch { case ex: Throwable => println ("Ex: " + ex.getMessage) } finally pw.close()
  }

  def loadXml(s: String): Node = {
    val factory = new SAXFactoryImpl()
    val loader = XML.withSAXParser(factory.newSAXParser())
    scala.xml.Utility.trim(loader.loadString(s))
  }

  def escapeJson(s: String): String = {
    val sb = new StringBuilder(Math.round(s.length.toFloat * 1.2f))
    var t: String = ""
    for ( c <- s ) c match {
      case '\\' => sb.append("\\\\");
      case '"' =>
        sb.append('\\')
        sb.append(c)
      case '/' =>
        sb.append('\\')
        sb.append(c);
      case '\b' =>
        sb.append("\\b")
      case '\t' =>
        sb.append("\\t")
      case '\n' =>
        sb.append("\\n")
      case '\f' =>
        sb.append("\\f")
      case '\r' =>
        sb.append("\\r")
      case _ =>
        if (c < ' ') {
          t = "000" + Integer.toHexString(c)
          sb.append("\\u" + t.substring(t.length() - 4))
        } else {
          sb.append(c)
        }
      }
    sb.toString
  }

  def time[R](block: => R)(label: String = ""): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    println(label + " Elapsed time: " + ((t1 - t0) * 0.000001) + "ms")
    result
  }
}

object UtilAliases {
  type ErrorMsg = String
  type Fallible[T] = Either[ErrorMsg, T]

  type Subjects = List[Subject]
  type Estimations = List[Estimation]
}