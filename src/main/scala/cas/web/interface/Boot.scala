package cas.web.interface

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import cas.analysis.estimation._
import cas.service.AProducer$
import cas.utils._
import cas.web.dealers.vk.VkApiDealer
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.routing.SimpleRoutingApp
import scala.concurrent.duration._
import scala.util.Try

object ImplicitActorSystem {
  implicit val system = ActorSystem("web-service")
}

object Boot extends App with SimpleRoutingApp {
  import ImplicitActorSystem._

	implicit val timeout = Timeout(10.seconds)
  val interface = system.actorOf(Props[AInterfaceControl], "interface-control")

  val config =  ConfigFactory.load()
  val addr = config.getString("cas.interface")
  val port = config.getInt("cas.port")

  println(s"Starting server on $addr:$port")

//  val dealer = new VkApiDealer(-29534144, None)
//  val estimator = new TotalEstimator(new LoyaltyEstimator(new LoyaltyConfigs(5, 0)) :: Nil)

  // val service = Props(new AContentService(dealer, estimator))

	 IO(Http) ? Http.Bind(interface, addr, port)
}