package cas.analysis.estimation

import java.util.Date

import scala.util.{Failure, Success}
import cas.utils.StdImplicits._
import cas.analysis.subject._
import cas.analysis.subject.components._
import org.joda.time.DateTime

case class LoyaltyConfigs(
      likesThresh: Double, repostThresh: Double, override val weight: Double = 1.0
  ) extends EstimatorConfigs(weight)

class LoyaltyEstimator(cfg: LoyaltyConfigs) extends ActualityEstimator(cfg) {
  val zeroReposts = new Virality(0.0)

  override def estimateActuality(subj: Subject): Either[String, Double] = for {
    likes <- subj.getComponent[Likability].right
    reposts = subj.getComponent[Virality].right.getOrElse(zeroReposts)
    date <- subj.getComponent[CreationDate].right
  } yield estimateLoyalty(likes.value, reposts.value, date.value)

  def estimateLoyalty(likes: Double, repost: Double, date: DateTime) =
    if (likes >= cfg.likesThresh || repost >= cfg.repostThresh) 1.0 else 0.0
}
