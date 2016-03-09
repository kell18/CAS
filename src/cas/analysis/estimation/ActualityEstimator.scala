package cas.estimation

import cas.subject._
import scala.util.Try

class EstimatorConfigs(val weight: Double = 1.0)

abstract class ActualityEstimator(val cfg: EstimatorConfigs) {
  def estimateActuality(subj: Subject): Try[Double]
}

case class ComponentNotFound(val estim: ActualityEstimator)
  extends Exception(estim.getClass.getName, null, false, false)