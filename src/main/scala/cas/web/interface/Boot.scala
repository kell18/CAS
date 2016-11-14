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
import scala.util.Try
import org.elasticsearch.common.settings.Settings

import scala.xml._
import scala.xml.factory.XMLLoader
import scala.xml.parsing.NoBindingFactoryAdapter
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import Utils._
import cas.persistence.SubjectsGrader
import cas.utils.Files._
import cas.utils.UtilAliases.ErrorMsg
import cas.web.pages.ControlPage

import scala.concurrent.duration._
import scala.xml.{Node, XML}
import cats.data.Validated.{invalid, valid}
import cats.std.all._
import cats.syntax.cartesian._

object ImplicitRuntime {
  implicit val system = ActorSystem("web-service")
  implicit val timeout = 10.seconds
}

import spray.client.pipelining._
object Boot extends App {
  import ImplicitRuntime._
  import system.dispatcher

  implicit val t = Timeout(timeout)
  val interface = system.actorOf(Props[AInterfaceControl], "interface-control")

  val config =  ConfigFactory.load()
  val addr = config.getString("cas.interface")
  val port = config.getInt("cas.port")

  println(s"Starting server on $addr:$port")

  IO(Http) ? Http.Bind(interface, addr, port)
  // transformData

  /*val a = (valid[String, Int](1) combine
           valid[String, Int](2) combine
           valid[String, Int](3)) map {_ + _ + _}

  println(a)*/

  def transformData = {
    val weights = Array(3.1642, -61.6405, -15.6417, -1.7404)
    val classifier = new SubjectsClassificator(weights,
      new StaticLoyaltyEstimator(StaticLoyaltyConfigs()),
      new ContinuousInvRelEstimator(ContinuousInvRelEstimatorConfigs(ControlPage.searcher)),
      new CorrectnessEstimator(CorrectnessConfigs()))

    val grader: SubjectsGrader = new SubjectsGrader()
    val path = Files.resources + "/cas/data/marked/marked_testing.json"
    for {
      // data <- grader.convertDumpToData(path)
      estims <- grader.convertDumpToEstimations(path)
    } yield {
      println(estims.length)
    }
  }

}
