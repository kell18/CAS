package cas.analysis.estimation

import cas.analysis.subject.Subject
import cas.analysis.subject.components.{Description, Likability}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
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

case class InverseRelevanceConfigs(
    searcher: SearchEngine, threshold: Float, override val weight: Double = 1.0
  ) extends EstimatorConfigs(weight)

/** Computes relevance of subject by scoring it relative to @ind / @spape index in elasticsearch */
class InvRelevanceEstimator(cfg: InverseRelevanceConfigs)(implicit val client: ElasticClient) extends ActualityEstimator(cfg) {

  override def estimateActuality(subj: Subject): Either[String, Double] = for {
    descr <- subj.getComponent[Description]
    resp <- Await.result(cfg.searcher.queryEntityScore(descr.text), 10.seconds)
    score = clampScore(resp.maxScore)
    likes = subj.getComponent[Likability].get.value
  } yield if (score > cfg.threshold) 1.0 else 0.0


  def clampScore(score: Option[Double]) = if (score.isEmpty) 0.0 else if (score.get == Double.NaN) 0.0 else score.get
}
