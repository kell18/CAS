package cas.utils

import StdImplicits._
import java.io.{File, FileNotFoundException}

import scala.io.Source
import scala.util.Try
import scala.xml.pull._

object Files {
  val resources = new File(".").getCanonicalPath + "/resources"
  val dealers = resources + "/cas/dealers"
  val currentDealer = dealers + "/UsingDealer.json"

  def writeToFile(path: String, content: String) = {
    val pw = new java.io.PrintWriter(new File(path))
    Try { pw.write(content) } eventually { pw.close() }
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
