package cas.analysis.estimation

import scala.util.{Failure, Success}
import cas.utils.StdImplicits._
import cas.analysis.subject._
import cas.analysis.subject.components._
import cas.utils.Utils
import org.joda.time.{Chronology, DateTime, DateTimeZone, Period}

case class LoyaltyConfigs(
    scores: Map[Period, Double], override val weight: Double = 1.0
  ) extends EstimatorConfigs(weight)

class LoyaltyEstimator(cfg: LoyaltyConfigs) extends ActualityEstimator(cfg) {

  override def estimateActuality(subj: Subject): Either[String, Double] = for {
    likes <- subj.getComponent[Likability]
    reposts = subj.getComponent[Virality].getOrElse(new Virality(0.0))
    date <- subj.getComponent[CreationDate]
  } yield estimateLoyalty(likes.value, reposts.value, date.value, subj)

  def estimateLoyalty(likes: Double, repost: Double, subjDate: DateTime, subj: Subject) = {
    val pastTime = new Period(subjDate, DateTime.now(Utils.timeZone))
    val isFail = cfg.scores.exists { case(period, minScore) =>
      pastTime.toStandardSeconds.getSeconds > period.toStandardSeconds.getSeconds && (likes + repost) < minScore
    }
    if (isFail) 0.0 else 1.0
  }
}
