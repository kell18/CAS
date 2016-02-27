package web.service

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._
// import web.service._

class ServiceControllSpec extends Specification with Specs2RouteTest with ServiceControll {
  def actorRefFactory = system
  
  "ServiceControll" should {

    "return a greeting for GET requests to the root path" in {
      Get() ~> route ~> check {
        responseAs[String] must contain("System")
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> route ~> check {
        handled must beFalse
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      Put() ~> sealRoute(route) ~> check {
        status === MethodNotAllowed
        responseAs[String] === "HTTP method not allowed, supported methods: GET"
      }
    }
  }
}
