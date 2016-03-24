package cas.estimation

import java.util.Date
import scala.util.{ Success, Failure }
import cas.utils.EitherImplicits._
import cas.subject._
import cas.subject.components._

case class LoyaltyConfigs(
      likesThresh: Double, repostThresh: Double, override val weight: Double = 1.0
  ) extends EstimatorConfigs(weight)

class LoyaltyEstimator(cfg: LoyaltyConfigs) extends ActualityEstimator(cfg) {
  val zeroReposts = new Virality(0.0)

  override def estimateActuality(subj: Subject): Either[String, Double] = for {
    likes <- subj.getComponent[Likability].right
    repost = subj.getComponent[Virality].right.getOrElse(zeroReposts)
    date <- subj.getComponent[CreationDate].right
  } yield estimateLoyalty(likes.value, repost.value, date.value)

  def estimateLoyalty(likes: Double, repost: Double, date: Date) =
    if (likes >= cfg.likesThresh || repost >= cfg.repostThresh) 1.0 else 0.0
}
