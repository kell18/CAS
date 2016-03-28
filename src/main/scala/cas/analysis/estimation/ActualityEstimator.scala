package cas.analysis.estimation

import cas.analysis.subject.Subject

class EstimatorConfigs(val weight: Double = 1.0)

abstract class ActualityEstimator(val cfg: EstimatorConfigs) {
  def estimateActuality(subj: Subject): Either[String, Double]
}