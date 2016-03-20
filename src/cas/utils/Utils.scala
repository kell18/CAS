package cas.utils

import java.io._
import com.typesafe.config._

object Utils {
  val dataPath = "resources/data"
  val confPath = "resources/conf"
  val webModelPath = dataPath + "/web.model"

  val configs = ConfigFactory.load(
    ConfigFactory.parseFile(new File(confPath + "/application.conf"))
  )

  def writeToFile(path: String, s: String): Unit = {
    println ("Write")
    val pw = new java.io.PrintWriter(new File(path))
    try { pw.write(s) } catch { case ex: Throwable => println ("Ex: " + ex.getMessage) } finally pw.close()
  }

  def time[R](block: => R)(label: String = ""): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    println(label + " Elapsed time: " + ((t1 - t0) * 0.000001) + "ms")
    result
  }
}