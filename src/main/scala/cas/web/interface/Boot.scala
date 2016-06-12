package cas.web.interface

import java.io.File
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import cas.analysis.estimation._
import cas.service.AProducer
import cas.utils._
import cas.web.dealers.vk.VkApiDealer
import com.typesafe.config.ConfigFactory
import spray.can.Http
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import org.elasticsearch.common.settings.Settings
import scala.xml._
import scala.xml.factory.XMLLoader
import scala.xml.parsing.NoBindingFactoryAdapter
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import Utils._
import akka.event.Logging
import cas.math.Optimization
import cas.persistence.{ASubjectsGrader, SubjectsGrader}
import cas.persistence.SubjectsGraderProtocol.{Data, Features}
import cas.utils.Files._
import cas.utils.UtilAliases.ErrorMsg
import cas.web.dealers.DealersFactory
import cas.web.pages.ConfigurePage
import scala.collection.immutable.NumericRange
import scala.concurrent.duration._
import scala.xml.{Node, XML}
/*import cats.data.Validated.{invalid, valid}
import cats.std.all._
import cats.syntax.cartesian._*/
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import cas.persistence.SubjectsGraderProtocol._

object ImplicitRuntime {
  implicit val system = ActorSystem("web-service")
  val timeout = 10.seconds
}

import spray.client.pipelining._
object Boot extends App {
  import ImplicitRuntime._
  import system.dispatcher

  StartTasks.transformData
}

object StartTasks {
  import ImplicitRuntime._

  def runServer = {
    implicit val timeout = Timeout(10.seconds)
    val interface = system.actorOf(Props[AInterfaceControl], "interface-control")

    val config =  ConfigFactory.load()
    val addr = config.getString("cas.interface")
    val port = config.getInt("cas.port")

    println(s"Starting server on $addr:$port")

    IO(Http) ? Http.Bind(interface, addr, port)
  }

  def gradeComments(amt: Int) = {
    val dealer = DealersFactory.buildDealer(VkApiDealer.id, ConfigurePage.searcher).get
    val grader = system.actorOf(Props(new ASubjectsGrader(dealer, amt)))
  }

  def exploreData = {
    val grader: SubjectsGrader = new SubjectsGrader(50)

    def relevanceCostFn(data: List[Data])(threshold: Double) = {
      var TP = 0.0
      var TN = 0.0
      var FP = 0.0
      var FN = 0.0
      data.foreach(d => {
        val predictedClass = if (d.features.relevance > threshold) 0 else if (d.features.likes > 6) 0 else 1
        if (predictedClass > d.isToDelete) {
          FP += 1.0
        } else {
          if (predictedClass != d.isToDelete) FN += 1.0
          else if (predictedClass == 1) TP += 1.0 else TN += 1.0
        }
      })
      val P = TP / (TP + FP)
      val R = TP / (TP + FN)
      println("Errs cnt: " + (FP + FN))
      println("Wrt cnt: " + (TP + TN))
      // 1.0 - (TP + TN) / (TP + TN + FP + FN)
      1.0 - (2.0 * P * R) / (P + R)
    }

    def correctnessCostFn(data: List[Data])(wordsMin: Int)(wordsMax: Int)(threshold: Double) = { // correctnessCostFn(data)(2)(180)(0.33)
      var TP = 0.0
      var TN = 0.0
      var FP = 0.0
      var FN = 0.0
      data.foreach { d =>
        val c = computeCorrectness(d.features, wordsMin, wordsMax)
        val predicted = if (c > threshold) 0 else 1
        if (predicted > d.isToDelete) {
          FP += 1.0
        } else {
          if (predicted != d.isToDelete) FN += 1.0
          else if (predicted == 1) TP += 1.0 else TN += 1.0
        }
      }
      val P = TP / (TP + FP)
      val R = TP / (TP + FN)
      println("Errs cnt: " + (FP + FN))
      println("Wrt cnt: " + (TP + TN))
      1.0 - (TP + TN) / (TP + TN + FP + FN)
      // (2.0 * P * R) / (P + R)
    }

    for {
      data <- grader.convertDumpToData(
        "C:\\Users\\Albert\\Code\\Scala\\CAS\\resources\\cas\\data\\marked\\marked_validation.json")
    } yield {
      val likes = data.map(_.features.likes)
      val likesAvg = likes.sum.toDouble / likes.length.toDouble
      val likesMax = likes.max.toDouble
      val r = data.map { d =>
        (printMultiClass(predictClass(d.features), d.isToDelete), postProcess(d.features, likesAvg, likesMax))
      } filter { p =>
        p._1 != "True"
      }
      val fpAmt = r.count(p => p._1 == "FP")
      val fnAmt = r.count(p => p._1 == "FN")
      println("fpAmt: " + fpAmt + " fnAmt: " + fnAmt)
      /*filter { p =>
        p._1 == "FP" && p._2.correct > 0.6
      } foreach { println }*/
    }

    /* tryData match {
      case Success(data) => //println(data.filter(i => i.features.wordsAmt > 150 || i.features.wordsAmt < 3).mkString("\n"))
        println("Data parsed, start optimization...")
        val rng = for (i <- 1 to 500) yield i / 1.0
        // println(correctnessCostFn(data)(3)(180)(0.66))
        def wordsThr(b: Double) = correctnessCostFn(data)(2)(b.toInt)(0.33)
        println(Optimization.findMin_brute(wordsThr, rng))
      case Failure(ex) => println(s"Err: `${ex.getMessage}`")
    }*/
  }

  def transformData = {
    val grader: SubjectsGrader = new SubjectsGrader(50)
    val legend = "Class,likes,relevance,correctness,dummy\n"
    val tryData = grader.convertDumpToData(
      "C:\\Users\\Albert\\Code\\Scala\\CAS\\resources\\cas\\data\\marked\\marked_train.json")


    tryData match {
      case Success(data) =>
        val likes = data.map(_.features.likes)
        val likesAvg = likes.sum.toDouble / likes.length.toDouble
        val likesMax = likes.max.toDouble
        Files.writeToFile(Files.static + "/data-train_oct.txt",
        /*legend + */(data.map { i =>
           /*printMultiClass(predictClass(i.features), i.isToDelete) + "," + */
          postProcess(i.features, likesAvg, likesMax).toTxt + " " + i.isToDelete
        } mkString "\n"))
      case Failure(ex) => println(s"Err: `${ex.getMessage}`")
    }
  }

  case class PostData(likes: Double, rel: Double, correct: Double) {
    def toCsv = List(likes, rel, correct).mkString(",")
    def toTxt = List(1.0, likes, rel, correct).mkString(" ")
  }

  def postProcess(f: Features, likesAvg: Double, likesMax: Double) = {
    val correctness = computeCorrectness(f)
    val l = (f.likes.toDouble - likesAvg) / likesMax + 0.1 - 0.0577638190954774
    PostData(l, f.relevance, correctness)
  }

  def computeCorrectness(f: Features, wordsMin: Int = 2, wordsMax: Int = 180) = {
    if (f.wordsAmt < wordsMin || f.wordsAmt > wordsMax) 0.0 // TODO: Pick coefs
    else {
      var sum = 0.33
      val punctPercent = Regexf.countItems(f.text, Regexf.mainPunctuation).toDouble / f.wordsAmt.toDouble
      if (punctPercent > 0.1 && punctPercent < 1.0) sum += 0.33
      // if (punctPercent > 0.9) println("punctPercent: " + punctPercent + " txt: " + f.text)
      val upperCasePercent = Regexf.countItems(f.text, Regexf.upperCase).toDouble / f.text.length.toDouble
      if (upperCasePercent < 0.5) sum += 0.33
      // if (upperCasePercent > 0.1) println("upperCasePercent: " + upperCasePercent + " txt: " + f.text)
      sum
    }
  }

  def predictClass(f: Features) = {
    val threshold = 0.088
    val correctness = computeCorrectness(f)
    if (f.relevance > threshold) 0 else if (f.likes > 6) 0 else (if (correctness > 0.66 && f.relevance > 0.4) 0 else 1)
  }

  def printMultiClass(predicted: Int, actual: Int) = {
    if (predicted > actual) "FP"
    else {
      if (predicted != actual) "FN"
      else "True" // if (actual == 0) "TN" else "TP"
    }
  }

  def printClass(clazz: Int) = {
    if (clazz == 0) "stay" else "delete"
  }
}