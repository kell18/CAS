package cas.analysis.estimation

import cas.analysis.subject.Subject
import cas.analysis.subject.components.Description
import cas.math.Mathf.sigmoid
import cas.utils.StdImplicits.RightBiasedEither
import cas.utils.UtilAliases.ErrorMsg

class SubjectsClassificator(weights: Array[Double],
                            loyaltyEstim: StaticLoyaltyEstimator,
                            relevanceEstim: ContinuousInvRelEstimator,
                            correctnessEstim: CorrectnessEstimator,
                            estimWeight: Double = 0.5,
                            threshold: Double = 0.5) extends ActualityEstimator(new EstimatorConfigs(estimWeight)) {
  require(weights.length > 3)

  override def estimateActuality(subj: Subject): Either[String, Double] = for {
    loyalty <- loyaltyEstim.estimateActuality(subj)
    rel <- relevanceEstim.estimateActuality(subj)
    corr <- correctnessEstim.estimateActuality(subj)
  } yield {
    1.0 - sigmoid(weights(0) + weights(1) * loyalty + weights(2) * rel + weights(3) * corr)
  }

  def predictClass(subj: Subject): Either[ErrorMsg, SubjectClass.Value] = for {
    loyalty <- loyaltyEstim.estimateActuality(subj)
    rel <- relevanceEstim.estimateActuality(subj)
    corr <- correctnessEstim.estimateActuality(subj)
  } yield {
    SubjectClass.fromBoolean(sigmoid(weights(0) + weights(1) * loyalty + weights(2) * rel + weights(3) * corr) >= threshold)
  }
}

object SubjectClass extends Enumeration {
  type SubjectClass = Value
  val delete = Value("delete")
  val stay = Value("stay")

  def fromBoolean(clazz: Boolean) = if (clazz) delete else stay
  def fromInt(clazz: Int) = if (clazz > 0) delete else stay
  def toInt(clazz: SubjectClass) = if (clazz == delete) 1 else 0
}

object ClassificationError extends Enumeration {
  import cas.analysis.estimation.SubjectClass.SubjectClass

  type ClassificationError = Value
  val TP = Value("TP")
  val TN = Value("TN")
  val FP = Value("FP")
  val FN = Value("FN")
  val True = Value("True")

  def fromSubjClass(predicted: SubjectClass, actual: SubjectClass) = {
    if (predicted == SubjectClass.delete && actual == SubjectClass.stay) FP
    else {
      if (predicted != actual) FN
      else if (actual == SubjectClass.stay) TN else TP
    }
  }
}