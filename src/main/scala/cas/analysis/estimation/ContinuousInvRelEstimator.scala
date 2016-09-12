package cas.analysis.estimation

import cas.analysis.subject.Subject
import cas.analysis.subject.components.{Description, Likability, Relevance}
import cas.utils.StdImplicits.RightBiasedEither
import cas.utils.StdImplicits.TryOps
import spray.client.pipelining._
import spray.http._
import spray.httpx.unmarshalling.Unmarshaller
import spray.json._
import MediaTypes.`application/json`
import cas.persistence.searching.SearchEngine
import cas.utils.UtilAliases.ErrorMsg
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

case class ContinuousInvRelEstimatorConfigs(
    searcher: SearchEngine, override val weight: Double = 1.0
  ) extends EstimatorConfigs(weight)

/** Computes relevance of subject by scoring it */
class ContinuousInvRelEstimator(cfg: ContinuousInvRelEstimatorConfigs) extends ActualityEstimator(cfg) {

  override def estimateActuality(subj: Subject): Either[String, Double] = {
    val currRelevance = subj.getComponent[Relevance]
    if (currRelevance.isRight) currRelevance.map(_.value)
    else for {
      descr <- subj.getComponent[Description]
      resp <- Await.result(cfg.searcher.queryEntityScore(descr.text), 10.seconds)
    } yield clampScore(resp.maxScore)
  }

  // TODO (1): Add NaN check
  def clampScore(score: Option[Double]) = if (score.isEmpty) 0.0 else score.get
}
