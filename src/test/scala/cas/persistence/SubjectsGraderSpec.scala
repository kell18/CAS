package cas.persistence

import akka.event.Logging
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import utils.AkkaToSpec2Scope
import cas.analysis.subject._
import cas.analysis.subject.components._
import cas.persistence.SubjectsGraderProtocol.Snapshot
import cas.utils.Files
import org.joda.time.DateTime

class SubjectsGraderSpec  extends Specification with NoTimeConversions  {
  sequential

  "SubjectsGrader" should {
    val postID = "post1"
    val cmtID = ID("cmt1")
    val rawCmtDescr = "Fancy comment"
    val cmtDescr = Description("""#$%^&*as;lfd \t\t \r\n\n\r<</ ** """ + rawCmtDescr)
    val cmtDate = CreationDate(new DateTime())
    val postDescr = Description("Some post descr")
    var path = ""

    "push subjects to file" in new AkkaToSpec2Scope {
      val grader = new SubjectsGrader(2)
      val subj = Subject(List(
        Article("post1", postDescr.text),
        cmtID,
        cmtDescr,
        cmtDate,
        Likability(10.0)
      ))
      val commit = grader.commitSubject(subj)
      path = grader.snapshotPath
      grader.pushToFile(Snapshot(grader.data.values.toList))
      commit must beRight
      val tryDatFile = Files.readFile(path)
      tryDatFile must beSuccessfulTry
      tryDatFile.get must contain(rawCmtDescr)
      tryDatFile.get must contain(postDescr.text)
      tryDatFile.get must contain("10")
    }

    "convert raw data to featured" in new AkkaToSpec2Scope {
      val grader = new SubjectsGrader(1)
      val tryFeaturedDat = grader.convertDumpToData(path)
      tryFeaturedDat must beSuccessfulTry
      val featuredDat = tryFeaturedDat.get
      featuredDat.length must beEqualTo(1)
      featuredDat.head.id must beEqualTo(postID + "_" + cmtID.value)
      featuredDat.head.isToDelete must beEqualTo(0)
      featuredDat.head.features.text must contain(rawCmtDescr)
      featuredDat.head.features.wordsAmt must beEqualTo(5)
    }
  }
}
