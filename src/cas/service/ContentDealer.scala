package cas.service

import cas.subject.Subject
import cas.utils.Utils.ErrorMsg
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

abstract class ContentDealer {
  def estimatedQueryFrequency: FiniteDuration
  def pullSubjectsChunk: Future[List[Subject]]
  def pushEstimation(estim: Estimation): Future[Any]
  def estimateChunkLim: Future[Double] // TODO: Remove
}
