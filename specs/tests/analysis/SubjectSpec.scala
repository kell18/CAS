package analysis

import java.util.Date

import cas.analysis.subject.components.ID
import cas.subject.Subject
import cas.subject.components.{CreationDate, Likability, Virality}
import org.specs2.mutable.Specification

import scala.reflect.ClassTag

class SubjectSpec extends Specification {

  "SubjectSpec" should {
    val date = new Date()
    val subj = new Subject(Likability(10.0) :: ID("ID") :: CreationDate(date) :: Nil)
    "Found added components" in {
      subj.getComponent[Likability].right.get.value must beCloseTo(10.0, 0.0001)
      subj.getComponent[ID].right.get.value mustEqual "ID"
      subj.getComponent[CreationDate].right.get.value mustEqual date
    }

    "Return names of not added components in left" in {
      val vTag = implicitly[reflect.ClassTag[Virality]]
      val cmpName = subj.getComponent[Virality].left.get
      println("[SubjectSpec] Name of not added component in left: " + cmpName)
      cmpName mustEqual vTag.runtimeClass.getSimpleName
    }
  }

}
