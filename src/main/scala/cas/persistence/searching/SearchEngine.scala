package cas.persistence.searching

import cas.persistence.searching.ElasticProtocol.SearchResponse
import cas.utils.UtilAliases._
import scala.concurrent.Future

// TODO: Extract pesistence interface
abstract class SearchEngine {
  def queryEntityScore(entity: => String): Future[Either[ErrorMsg, SearchResponse]]
  def pushEntity(id: String, entity: => String): Future[Either[ErrorMsg, Any]]
  def delEntity(id: String): Future[Either[ErrorMsg, Any]]
  def initStorage: Future[Either[ErrorMsg, Boolean]]
  def disposeStorage: Future[Either[ErrorMsg, Boolean]]
}
