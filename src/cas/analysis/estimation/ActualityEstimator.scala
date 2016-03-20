package cas.estimation

import cas.subject._
import cas.subject.components.Component
import scala.util.Try
import scalaz._
import scalaz.Scalaz._

class EstimatorConfigs(val weight: Double = 1.0)

abstract class ActualityEstimator(val cfg: EstimatorConfigs) {
  def estimateActuality(subj: Subject): Either[String, Double]
}

case class ComponentNotFound(val estim: ActualityEstimator)
  extends Exception(estim.getClass.getName, null, false, false)