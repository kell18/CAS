package cas.analysis.estimation

import cas.analysis.subject.Subject
import cas.analysis.subject.components.{CreationDate, Likability, Virality}
import cas.utils.StdImplicits.RightBiasedEither
import cas.utils.UtilAliases.ErrorMsg

// TODO(CARE): This coefs work with certain polynom weights, need to change it with polynom
// TODO(5): Train regression on generic likesAvg and likesMax
case class StaticLoyaltyConfigs(likesAvg: Double = 8.25, likesMax: Double = 136.0,
    override val weight: Double = 1.0
  ) extends EstimatorConfigs(weight)

class StaticLoyaltyEstimator(cfg: StaticLoyaltyConfigs) extends ActualityEstimator(cfg) {

  override def estimateActuality(subj: Subject): Either[ErrorMsg, Double] = for {
    likes <- subj.getComponent[Likability]
    // reposts = subj.getComponent[Virality].getOrElse(new Virality(0.0)) // TODO: take into account
  } yield {
    math.min((likes.value - cfg.likesAvg) / cfg.likesMax + 0.060667001843472436, 1.0)
  }

}
