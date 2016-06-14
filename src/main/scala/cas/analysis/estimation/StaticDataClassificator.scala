package cas.analysis.estimation

import cas.analysis.subject.Subject
import cas.math.Mathf._
import cas.utils.UtilAliases._
import cas.utils.StdImplicits.RightBiasedEither

/**
  * Classificator for testing data mainly, using static relevance
  */
class StaticDataClassificator(weights: Array[Double],
                              loyaltyEstim: StaticLoyaltyEstimator,
                              correctnessEstim: CorrectnessEstimator) {
  require(weights.length > 3)

  val weights_loyalty = Array(0.46980, -104.42032)
  val weights_loyaltyAndRel = Array(0.94742,  -6.38366, -10.03590)

  def predictClass(subj: Subject, relevance: Double): Either[ErrorMsg, SubjectClass.Value] = for {
    loyalty <- loyaltyEstim.estimateActuality(subj)
    corr <- correctnessEstim.estimateActuality(subj)
  } yield {
    SubjectClass.fromBoolean(
      sigmoid(weights(0) + weights(1) * loyalty + weights(2) * relevance + weights(3) * corr) >= 0.5)
  }

  def predictClass_loyalty(subj: Subject): Either[ErrorMsg, SubjectClass.Value] = for {
    loyalty <- loyaltyEstim.estimateActuality(subj)
  } yield {
    SubjectClass.fromBoolean(sigmoid(weights_loyalty(0) + weights_loyalty(1) * loyalty) >= 0.5)
  }

  def predictClass_loyaltyAndRel(subj: Subject, relevance: Double): Either[ErrorMsg, SubjectClass.Value] = for {
    loyalty <- loyaltyEstim.estimateActuality(subj)
  } yield {
    SubjectClass.fromBoolean(
      sigmoid(weights_loyaltyAndRel(0) + weights_loyaltyAndRel(1) * loyalty + weights_loyaltyAndRel(2) * relevance) >= 0.5)
  }
}
