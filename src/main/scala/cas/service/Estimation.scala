package cas.service

import cas.analysis.subject.Subject

// TODO: Move to estimation
case class Estimation(subj: Subject, actuality: Double)

object Estimation {
  type Estimations = List[Estimation]
}
