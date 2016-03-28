package cas.web.pages

import cas.web.interface.PageUrls
import spray.routing.Directives._

object Auth {

	def apply(pagePath: String) = path(pagePath) {
    get {
      complete {
        <html>
          <body>
            <h4>Choos content dealer:</h4>
            <ul>
              <li><a href={PageUrls.authVk}>Vk API</a></li>
            </ul>
          </body>
        </html>
      }
    }
  }

}