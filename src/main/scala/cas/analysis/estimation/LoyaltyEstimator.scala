package cas.analysis.estimation

import scala.util.{Failure, Success}
import cas.utils.StdImplicits._
import cas.analysis.subject._
import cas.analysis.subject.components._
import cas.utils.Utils
import org.joda.time.{Chronology, DateTime, DateTimeZone, Period}

/** @param scores - param that determ like wight by period of time  */
case class LoyaltyConfigs(
  scores: Map[Period, Double], override val weight: Double = 1.0
) extends EstimatorConfigs(weight)

class LoyaltyEstimator(cfg: LoyaltyConfigs) extends ActualityEstimator(cfg) {

  override def estimateActuality(subj: Subject): Either[String, Double] = for {
    likes <- subj.getComponent[Likability]
    reposts = subj.getComponent[Virality].getOrElse(new Virality(0.0))
    date <- subj.getComponent[CreationDate]
  } yield estimateLoyalty(likes.value, reposts.value, date.value, subj)

  /** Estimate loyalty based on likes weight in period of time (scores) */
  def estimateLoyalty(likes: Double, repost: Double, subjDate: DateTime, subj: Subject) = {
    val pastTime = new Period(DateTime.now(Utils.timeZone), subjDate).toStandardSeconds.getSeconds
    var loyalty = 0.0
    cfg.scores.foldLeft(Period.ZERO -> cfg.scores.head._2) ((prev, next) => { // Fr zero
      val prevTime = prev._1.toStandardSeconds.getSeconds
      val nextTime = next._1.toStandardSeconds.getSeconds
      if (pastTime >= prevTime && pastTime < nextTime) {
        loyalty = if (prev._1 == Period.ZERO) 1.0 else Math.min(likes * prev._2, 1.0)
      }
      (next._1, next._2)
    })
    val last = cfg.scores.last
    if (pastTime > last._1.toStandardSeconds.getSeconds) loyalty = likes * last._2
    loyalty
  }

}
