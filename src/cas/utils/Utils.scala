package utils

import java.io._
import com.typesafe.config._

object Utils {
  val dataPath = "resources/data"
  val confPath = "resources/conf"
  val webModelPath = dataPath + "/web.model"

  val configs = ConfigFactory.load(
    ConfigFactory.parseFile(new File(confPath + "/application.conf"))
  )

  def writeToFile(p: String, s: String): Unit = {
    val pw = new java.io.PrintWriter(new File(p))
    try pw.write(s) finally pw.close()
  }
}