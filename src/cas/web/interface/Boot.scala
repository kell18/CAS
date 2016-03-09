package web.interface

import java.io._
import java.util.Calendar

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

import web.model.UserSettingsProtocol._
import web.model._
import utils._
import cas.subject._
import cas.estimation._
import cas.subject.components._

import scala.util.{ Try, Success, Failure }

object Boot extends App with SimpleRoutingApp {
	implicit val timeout = Timeout(5.seconds)

	implicit val system = ActorSystem("web-service")

	val interface = system.actorOf(Props[AInterfaceControll], "interface-controll")

  val addr = Utils.configs.getString("cas.interface");
  val port = Utils.configs.getInt("cas.port");

  // TODO: Move to Specs.
  // val s = new Subject(Set(
  //   new Likeability(30.0),
  //   new Virality(2.0),
  //   new CreationDate(Calendar.getInstance.getTime)
  // ))
  // val l = s.getComponent[Likeability]
  // val v = s.getComponent[Virality]
  // val d = s.getComponent[CreationDate]
  // println("Likeability " + l.get.value)
  // println("Virality " + v.get.value)
  // println("Date " + d.get.value)

  // val estimator = new LoyalityEstimator(25, 3)
  // println("Loyality " + estimator.estimateActuality(s).get)

  // List[EstimatorUnit] estimators = // ... 
  // val actuality = estimators.foldLeft(0)((l, r) => l + r.v * r.w)

  println("Starting CAS on " + addr + ":" + port)

	// IO(Http) ? Http.Bind(interface, addr, port)
}