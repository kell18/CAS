package cas.web.pages

import spray.routing._
import Directives._

object ConfigurePage {

	def apply(pagePath: String) = path(pagePath){
		get	{
			parameter("threshold".as[Int]) { threshold =>
				complete {
					<span>Likes removal threshold = {threshold}</span>
				}
			}
		}
	}

}