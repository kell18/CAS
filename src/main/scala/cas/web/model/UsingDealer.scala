package cas.web.model

import spray.json.DefaultJsonProtocol

object UsingDealerProtocol extends DefaultJsonProtocol {
  implicit val usingDealerFormat = jsonFormat1(UsingDealer)
}

case class UsingDealer(id: String)
