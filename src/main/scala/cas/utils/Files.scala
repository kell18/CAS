package cas.utils

import StdImplicits._
import java.io.File

import scala.util.{Success, Try}

object Files {
  val resources = new File(".").getCanonicalPath + "/resources"
  val dealers = resources + "/cas/dealers"
  val currentDealer = dealers + "/UsingDealer.json"

  def writeToFile(path: String, content: String) = {
    val file = new File(path)
    val tryFile = if (!file.exists()) Try(file.createNewFile()) else Success(true)
    for {
      f <- tryFile
      pw = new java.io.PrintWriter(file)
      result <- Try { pw.write(content) } eventually { pw.close() }
    } yield result
  }

  def readFile(path: String) = for {
    src <- Try(scala.io.Source.fromFile(path))
    str <- Try(src.mkString) eventually { src.close }
  } yield str

  // TODO: Bug here - closing stream
  def readFileLines(path: String) = {
    val source = scala.io.Source.fromFile(path)
    Try { source.getLines } eventually { source.close }
  }
}
