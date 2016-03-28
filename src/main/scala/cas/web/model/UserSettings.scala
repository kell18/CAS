package cas.web.model

import scala.io.Source
import spray.json._
import cas.utils._

case class UserSettings(likesThreshold: Int)

object UserSettings {
  import UserSettingsProtocol._

  def read: Option[String] = {
    val s = Source.fromFile(Utils.webModelPath + "/UserSettings.json").mkString
    if (s.trim.isEmpty) None else Some(s)
  }

  def update(userSettings: String) = {
    Utils.writeToFile(Utils.webModelPath + "/UserSettings.json", userSettings)
  }
}

object UserSettingsProtocol extends DefaultJsonProtocol {
  implicit val userSettingsFormat = jsonFormat1(UserSettings.apply)
}