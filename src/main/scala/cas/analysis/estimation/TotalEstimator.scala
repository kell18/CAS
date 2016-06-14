package cas.analysis.estimation

import cas.analysis.subject.Subject
import cas.analysis.subject.components.Description
import scala.util.{Failure, Success}
import cas.utils.StdImplicits.RightBiasedEither

class TotalEstimator(val estimators: List[ActualityEstimator]) extends ActualityEstimator(new EstimatorConfigs()) {

  override def estimateActuality(subj: Subject): Either[String, Double] = {
    var totalEstim = 0.0
    var missComponents: List[String] = Nil
    for (estim <- estimators) {
      val actuality = estim.estimateActuality(subj)
      if (actuality.isRight) totalEstim += actuality.right.get * estim.cfg.weight
      else missComponents = estim.getClass.getSimpleName + ": " + actuality.left.get :: missComponents
    }
    if (missComponents.isEmpty) Right(totalEstim) else Left(missComponents.mkString(", "))
  }

}
