package cas.web.interface

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
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl.{get, _}
import com.sksamuel.elastic4s.analyzers.{CustomAnalyzerDefinition, RussianLanguageAnalyzer, StopAnalyzer}
import com.sksamuel.elastic4s.mappings.FieldType.{IntegerType, StringType}
import org.elasticsearch.common.settings.Settings

import scala.xml._
import scala.xml.factory.XMLLoader
import scala.xml.parsing.NoBindingFactoryAdapter
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import Utils._
import spray.http.{HttpRequest, HttpResponse, Uri}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.xml.{Node, XML}

object ImplicitRuntime {
  implicit val system = ActorSystem("web-service")
  implicit val client = ElasticClient.transport(
    Settings.settingsBuilder().put("cluster.name", "cas-app").build(),
    ElasticsearchClientUri("127.0.0.1", 9300)
  )
}

import spray.client.pipelining._
object Boot extends App {
  import ImplicitRuntime._
  import system.dispatcher


  /*val ind = "rbc"
  val shape = "posts"

  implicit val timeout = Timeout(10.seconds)
  val interface = system.actorOf(Props[AInterfaceControl], "interface-control")

  val config =  ConfigFactory.load()
  val addr = config.getString("cas.interface")
  val port = config.getInt("cas.port")

  println(s"Starting server on $addr:$port")

  IO(Http) ? Http.Bind(interface, addr, port)*/

  val url = "http://localhost:9201/rbc/posts/_search"
  val data = "{ \"query\": { \"match\": { \"text\": { \"analyzer\": \"russian\", \"query\": \"Сейчас раскатают так что виновен будет ТОЧНО тот кто с огнестрелом.\" } } } }"
  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  // Post with header and parameters
  val resp: Future[String] = pipeline(Get(Uri(url), data)) map (_.entity.asString)

  println("resp: " + Await.result(resp, 10.seconds))

  //



}
