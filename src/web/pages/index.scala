package web.pages

import spray.routing._
import Directives._

object IndexPage {

	val route = path("") {
      get {
        complete {
          <html>
            <body>
              <h2>Content Analysis System</h2>
            </body>
          </html>
        }
      }
    }
    
}