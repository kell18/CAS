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
import cas.utils.UtilAliases.ErrorMsg
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

case class InverseRelevanceConfigs(
    threshold: Float, override val weight: Double = 1.0
  ) extends EstimatorConfigs(weight)

/** Computes relevance of subject by scoring it relative to @ind / @spape index in elasticsearch */
class InvRelevanceEstimator(cfg: InverseRelevanceConfigs)(implicit val client: ElasticClient) extends ActualityEstimator(cfg) {
  val ind = "rbc"
  val shape = "posts"
  override def estimateActuality(subj: Subject): Either[String, Double] = for {
    descr <- subj.getComponent[Description]
    resp <- Try(querySubjectDescr(descr.text)).asEitherString
    score = clampScore(resp.maxScore)
    likes = subj.getComponent[Likability].get.value
  } yield if (score > cfg.threshold) 1.0 else 0.0

  def querySubjectDescr(txt: String) = {
//   client.java.prepareSearch(ind).setQuery(s"""{ "match": { "text" : "$txt" } }""").execute().get()
    val r = client.execute { search in ind / shape  query "text" -> txt }.await
    Thread.sleep(1000)
    r
  }

  def clampScore(score: Float) = if (score < 0.01f) 0.0f else score
}
