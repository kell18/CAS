package web.pages

import spray.routing._
import Directives._

object ConfigurePage {

	val route = path("configure") {
		get	{
			parameter("threshold".as[Int]) { threshold =>
				complete {
					<span>Threshold = {threshold}</span>
				}
			}
		}
	}

}