package cas.analysis.estimation

import cas.analysis.subject.Subject
import cas.analysis.subject.components.Attachments
import cas.utils.StdImplicits.RightBiasedEither
import cas.utils.Regexf

case class AttachmentsConfigs(override val weight: Double = 1.0
  ) extends EstimatorConfigs(weight)

class AttachmentsEstimator(cfg: AttachmentsConfigs) extends ActualityEstimator(cfg) {

  override def estimateActuality(subj: Subject): Either[String, Double] = for {
    attachs <- subj.getComponent[Attachments]
  } yield if (attachs.kinds.nonEmpty) 0.0 else 1.0

}
