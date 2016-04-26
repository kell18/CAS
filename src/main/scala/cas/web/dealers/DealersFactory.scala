package cas.web.dealers

import cas.persistence.searching.SearchEngine
import cas.service.ContentDealer
import cas.utils.Files
import cas.web.dealers.vk.VkApiDealer
import scala.util.Try

object DealersFactory {
  import cas.web.interface.ImplicitRuntime._

  // TODO: Pass searcher as cfg (something like enum in json)
  def buildDealer(id: String, searcher: SearchEngine): Try[ContentDealer] = for {
    conf <- Files.readFile(Files.dealers + "/" + id)
    dealer <- dealersLookup(id)(conf, searcher)
  } yield dealer

  private val dealersLookup: Map[String, (String, SearchEngine) => Try[ContentDealer]] = Map(
    VkApiDealer.id -> VkApiDealer.apply
  )
}
