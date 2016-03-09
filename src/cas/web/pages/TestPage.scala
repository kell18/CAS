package web.pages

import scala.io.Source
import spray.routing._
import spray.json._
import Directives._
import utils._
import web.model._
import web.model.UserSettingsProtocol._

object TestPage {

  def apply(pagePath: String) = path(pagePath / IntNumber / IntNumber) { (a, b) =>
    complete(s"The result is ${a / b}")
  }

}