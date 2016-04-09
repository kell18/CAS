package cas.analysis.estimation

import cas.analysis.subject.Subject
import cas.analysis.subject.components.{Description, Likability}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import cas.utils.StdImplicits.RightBiasedEither
import cas.utils.StdImplicits.TryOps

import scala.util.Try

case class InverseRelevanceConfigs(
    threshold: Float, override val weight: Double = 1.0
  ) extends EstimatorConfigs(weight)

class InverseRelevanceEstimator(cfg: InverseRelevanceConfigs)(implicit val client: ElasticClient) extends ActualityEstimator(cfg) {
  val ind = "rbc"
  val shape = "posts"

  override def estimateActuality(subj: Subject): Either[String, Double] = for {
    descr <- subj.getComponent[Description]
    resp <- Try(querySubjectDescr(descr.text)).asEitherString
    score = clampScore(resp.maxScore)
    likes = subj.getComponent[Likability].get.value
    _ = if (score > cfg.threshold) println("Relevance: `" + score + "` Likes: `" + likes + "` Comment: `" + descr)
    _ = if (score > 1.5 && likes < 5) println("!!! Relevance: `" + score + "` Likes: `" + likes + "` Comment: `" + descr)
  } yield if (score > cfg.threshold) 1.0 else 0.0

  def querySubjectDescr(txt: String) = {
    val r = client.java.prepareSearch(ind).setQuery(s"""{ "match": { "text" : "$txt" } }""").execute().get()
    // println(r)
    r
//    client.java.prepareSearch(ind).setQuery(s"""{ "match": { "text" : "$txt" } }""").execute().actionGet()
  }

  def clampScore(score: Float) = if (score < 0.01f) 0.0f else score
}
