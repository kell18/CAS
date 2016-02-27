package web.service

import akka.actor.Actor
import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import web.pages._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class AServiceControll extends Actor with ServiceControll {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  def receive = runRoute(route)
}


// this trait defines our service behavior independently from the service actor
trait ServiceControll extends HttpService {

  val route = respondWithMediaType(`text/html`) {
    IndexPage.route ~
    ConfigurePage.route
	}
}