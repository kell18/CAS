package cas.estimation

import java.util.Date
import scala.util.{ Success, Failure }
import scalaz._
import scalaz.Scalaz._
import cas.subject._
import cas.subject.components._

case class LoyaltyConfigs(
      val likesThresh: Double, val repostThresh: Double, override val weight: Double = 1.0
  ) extends EstimatorConfigs(weight)

class LoyaltyEstimator(cfg: LoyaltyConfigs) extends ActualityEstimator(cfg) {

  override def estimateActuality(subj: Subject): Either[String, Double] = for {
    likes <- subj.getComponent[Likability]
    repost <- subj.getComponent[Virality]
    date <- subj.getComponent[CreationDate]
  } yield estimateLoyalty(likes.value, repost.value, date.value)

  def estimateLoyalty(likes: Double, repost: Double, date: Date) =
    if (likes >= cfg.likesThresh || repost >= cfg.repostThresh) 1.0 else 0.0
}
