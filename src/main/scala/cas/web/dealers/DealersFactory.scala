package cas.web.dealers

import cas.service.ContentDealer
import cas.utils.Files
import cas.web.dealers.vk.VkApiDealer

import scala.util.Try

object DealersFactory {
  import cas.web.interface.ImplicitRuntime._

  val dealersLookup: Map[String, String => Try[ContentDealer]] = Map(
    VkApiDealer.id -> VkApiDealer.apply
  )

  def buildDealer(id: String): Try[ContentDealer] = for {
    conf <- Files.readFile(Files.dealers + "/" + id)
    dealer <- dealersLookup(id)(conf)
  } yield dealer
}
