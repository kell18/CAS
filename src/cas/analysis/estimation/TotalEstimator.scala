package cas.estimation

import cas.subject.Subject
import scala.util.{ Try, Success, Failure }

class TotalEstimator(val estimators: List[ActualityEstimator]) extends ActualityEstimator(new EstimatorConfigs()) {
  override def estimateActuality(subj: Subject): Try[Double] = {
    var totalEstimation = 0
    for (est <- estimators) {
      val act = est.estimateActuality(subj)
      if (act.isFailure) return Failure(act.failed.get)
      else totalEstimation += act.get * est.cfg.weight
    }
    Success(totalEstimation)
  }
}
