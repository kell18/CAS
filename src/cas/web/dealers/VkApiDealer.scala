package web.dealers

import cas.service.{Estimation, ContentDealer}
import cas.subject.Subject
import scala.concurrent.Future

class VkApiDealer(val groupId: Long, val token: Long) extends ContentDealer {
  override def estimateChunksLim: Long = ???
  override def pullSubjectsChunk: Future[List[Subject]] = ???
  override def pushEstimationsChunk(estims: List[Estimation]) = ???
}
