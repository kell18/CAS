package cas.persistence.searching

import cas.utils.UtilAliases._
import scala.concurrent.Future

// TODO: Extract pesistence interface
abstract class SearchEngine {
  def queryEntityScore(entity: => String): Double
  def pushIndexedEntity(id: String, entity: => String): Future[Either[ErrorMsg, Any]]
  def delIndexedEntity(id: String): Future[Either[ErrorMsg, Any]]
}
