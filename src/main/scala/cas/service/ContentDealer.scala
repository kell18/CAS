package cas.service

import cas.analysis.subject.Subject
import cas.utils.Utils.{ErrorMsg, Fallible}
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import cas.analysis.subject.Subject._

abstract class ContentDealer {
  def estimatedQueryFrequency: FiniteDuration
  def pullSubjectsChunk: Future[Either[ErrorMsg, Subjects]]
  def pushEstimation(estim: Estimation): Future[Either[ErrorMsg, Any]]
}
