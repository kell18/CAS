package cas.service

import cas.subject.Subject

import scala.concurrent.Future

abstract class ContentDealer {
  def estimateChunksLim: Long
  def pullSubjectsChunk: Future[List[Subject]]
  def pushEstimationsChunk(estims: List[Estimation])
}
