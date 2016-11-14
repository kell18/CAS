package cas.analysis.estimation

import cas.analysis.subject.Subject
import cas.analysis.subject.components.Description
import scala.util.{Failure, Success}
import cas.utils.StdImplicits.RightBiasedEither

class TotalEstimator(val estimators: List[ActualityEstimator],
                     val attachmentsEstimatorOpt: Option[AttachmentsEstimator] = None)
                extends ActualityEstimator(new EstimatorConfigs()) {
  val maxActuality = estimators.map(_.cfg.weight).sum

  override def estimateActuality(subj: Subject): Either[String, Double] = {
    if (attachmentsEstimatorOpt.isDefined) {
      for {
        estim <- attachmentsEstimatorOpt.get.estimateActuality(subj)
        if estim < 0.0
      } return Right(estim)
    }
    var totalEstim = 0.0
    var missComponents: List[String] = Nil
    for (estim <- estimators) {
      val actuality = estim.estimateActuality(subj)
      if (actuality.isRight) totalEstim += actuality.right.get * estim.cfg.weight
      else missComponents = estim.getClass.getSimpleName + ": " + actuality.left.get :: missComponents
      // println("!!! estim: " + estim.getClass.getName + " estimation: " + (actuality.right.get * estim.cfg.weight) + " descr: " + subj.getComponent[Description].get.text)
    }
    if (missComponents.isEmpty) Right(totalEstim / maxActuality) else Left(missComponents.mkString(", "))
  }

}
