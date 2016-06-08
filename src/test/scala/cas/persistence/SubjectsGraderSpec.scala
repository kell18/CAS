package cas.persistence

import akka.event.Logging
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import utils.AkkaToSpec2Scope
import cas.analysis.subject._
import cas.analysis.subject.components._
import cas.utils.Files
import org.joda.time.DateTime

class SubjectsGraderSpec  extends Specification with NoTimeConversions  {
  sequential

  "SubjectsGrader" should {
    "push subjects to file" in new AkkaToSpec2Scope {
      val grader = new SubjectsGrader(1)(Logging.getLogger(system, "SubjectsGrader"))
      val cmtID = ID("cmt1")
      val cmtDescr = Description("""#$%^&*as;lfd \t\t \r\n\n\r<</ ** Fancy comment\n""")
      val cmtDate = CreationDate(new DateTime())
      val postDescr = Description("Some post descr")
      val subj = Subject(List(
        Subject(List(ID("post1"), postDescr)),
        cmtID,
        cmtDescr,
        cmtDate,
        Likability(10.0)
      ))
      val commit = grader.commitSubject(subj)
      commit must beRight
      val tryDatFile = Files.readFile(grader.snapshotPath)
      tryDatFile must beSuccessfulTry
      tryDatFile.get must contain("Fancy comment")
      tryDatFile.get must contain(postDescr.text)
      tryDatFile.get must contain("10")
    }
  }
}
