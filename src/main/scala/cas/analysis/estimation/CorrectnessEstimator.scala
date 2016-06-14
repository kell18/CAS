package cas.analysis.estimation

import cas.analysis.subject.Subject
import cas.analysis.subject.components.Description
import cas.utils.StdImplicits.RightBiasedEither
import cas.utils.Regexf

case class CorrectnessConfigs(
    wordsMinAmt: Int = 2, wordsMaxAmt: Int = 180, punctMinAmt: Double = 0.1, punctMaxAmt: Double = 1.0,
    upperMaxAmt: Double = 0.5, override val weight: Double = 1.0
  ) extends EstimatorConfigs(weight)

class CorrectnessEstimator(cfg: CorrectnessConfigs) extends ActualityEstimator(cfg) {

  override def estimateActuality(subj: Subject): Either[String, Double] = for {
    descr <- subj.getComponent[Description]
  } yield computeCorrectness(descr.text)

  def computeCorrectness(text: String) = {
    val wordsAmt = Regexf.cleanAndSplit(text).length
    if (wordsAmt < cfg.wordsMinAmt || wordsAmt > cfg.wordsMaxAmt) 0.0
    else {
      var sum = 0.33
      val punctPercent = Regexf.countItems(text, Regexf.mainPunctuation).toDouble / wordsAmt.toDouble
      if (punctPercent > cfg.punctMinAmt && punctPercent < cfg.punctMaxAmt) sum += 0.33
      val upperCasePercent = Regexf.countItems(text, Regexf.upperCase).toDouble / text.length.toDouble
      if (upperCasePercent < cfg.upperMaxAmt) sum += 0.33
      sum
    }
  }

}
