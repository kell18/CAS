package cas.web.service

import java.io._

import scala.io.Source
import scala.concurrent.duration._
import scala.util.parsing.json._
import scala.util.{ Try, Success, Failure }

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import spray.can.Http
import spray.routing.SimpleRoutingApp
import spray.json._

import cas.web.model.UserSettingsProtocol._
import cas.web.model._
import cas.utils._
import cas.model.subject._
import cas.model.subject.components._

import scala.util.{ Try, Success, Failure }

object Boot extends App with SimpleRoutingApp {
	implicit val timeout = Timeout(5.seconds)

	implicit val system = ActorSystem("web-service")

	val service = system.actorOf(Props[AServiceControll], "service-controll")

  val interface = Utils.configs.getString("cas.interface");
  val port = Utils.configs.getInt("cas.port");

  // val s = new Subject((new Likeability(10.0)) :: Nil)
  // val l = s.getComponent[Likeability]
  // println(l.get.value)

  println("Starting CAS on " + interface + ":" + port)

	IO(Http) ? Http.Bind(service, interface, port)
}