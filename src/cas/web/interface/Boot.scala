package cas.web.interface

import java.io._
import java.util.Calendar

import cas.web.dealers.VkApiDealer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.routing.SimpleRoutingApp
import cas.utils._
import cas.subject._
import cas.estimation._
import cas.service.AContentService
import cas.subject.components._

import scala.concurrent.Await
import scala.util.{Failure, Success, Try}

object ImplicitActorSystem {
  implicit val system = ActorSystem("web-service")
}

object Boot extends App with SimpleRoutingApp {
  import ImplicitActorSystem._

	implicit val timeout = Timeout(10.seconds)
	val interface = system.actorOf(Props[AInterfaceControl], "interface-controll")

  val addr = Utils.configs.getString("cas.interface")
  val port = Utils.configs.getInt("cas.port")

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

  val dealer = new VkApiDealer(-29534144, None)
  val estimator = new TotalEstimator(new LoyaltyEstimator(new LoyaltyConfigs(5, 0)) :: Nil)

  val service = Props(new AContentService(dealer, estimator))

	IO(Http) ? Http.Bind(interface, addr, port)
}