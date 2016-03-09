package web.pages

import scala.io.Source
import scala.util.{ Try, Success, Failure }
import spray.routing._
import spray.json._
import Directives._
import utils._
import web.model._
import web.model.UserSettingsProtocol._

object IndexPage {

	def apply(pagePath: String) = path(pagePath) {
    get {
      parameter("threshold".?) { threshold =>
        lazy val fileSettings = Try(UserSettings.read.get.parseJson.convertTo[UserSettings])
        val usrSettings = if (!threshold.isEmpty) Try(UserSettings(threshold.get.toInt)) else fileSettings
        val settings = usrSettings getOrElse UserSettings(20)
        UserSettings.update(settings.toJson.prettyPrint)

        complete {
          <html>
            <body>
              <h2>Content Analysis System</h2>
              <form method="GET" action="">
                <p>Current likes threshold:</p> 
                { if (usrSettings.isFailure) <i>Something goes wrong, will use default settings</i> }
                <p><input type="text" name="threshold" value={settings.likesThreshold.toString} /></p>
                <p><input type="submit" value="Update" /></p>
              </form>
            </body>
          </html>
        }
      }
    }
  }

}