package cas.analysis

import java.util.Date
import cas.analysis.estimation._
import cas.utils.StdImplicits.RightBiasedEither
import cas.analysis.subject.Subject
import cas.persistence.SubjectsGrader
import cas.web.pages.ControlPage
import cas.analysis.subject.components.{CreationDate, Likability, Virality}
import cas.utils.Files
import org.joda.time.{DateTime, Duration, Period}
import org.specs2.mutable.Specification

class SubjectsClassificatorSpec extends Specification {
  "SubjectsClassificator" should {
    val weights = Array(3.1642, -61.6405, -15.6417, -1.7404)
    val classifier = new SubjectsClassificator(weights,
      new StaticLoyaltyEstimator(StaticLoyaltyConfigs()),
      new ContinuousInvRelEstimator(ContinuousInvRelEstimatorConfigs(ControlPage.searcher)),
      new CorrectnessEstimator(CorrectnessConfigs()))
    val parser = new SubjectsGrader()
    val tryEstims = parser.convertDumpToEstimations(Files.resources + "/cas/data/marked/marked_testing.json")


    "assign right class mark" in {
      val a = for (estims <- tryEstims)
        yield {
          for { e <- estims } yield {
            val cl = (1 - SubjectClass.toInt(classifier.predictClass(e.subj).get)).toDouble
            cl == e.actuality
          }
        }
      a.get must not contain false
    }

  }
}
