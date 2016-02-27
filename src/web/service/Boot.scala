package web.service

import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import spray.can.Http
import spray.routing.SimpleRoutingApp


object Boot extends App with SimpleRoutingApp {
	implicit val timeout = Timeout(5.seconds)

	implicit val system = ActorSystem("web-service")

	val service = system.actorOf(Props[AServiceControll], "service-controll")

	// start a new HTTP server on port 8080 with our service actor as the handler
	IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
}