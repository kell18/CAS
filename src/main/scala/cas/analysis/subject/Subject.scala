package cas.analysis.subject

import cas.utils.StdImplicits._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import cas.analysis.subject.components._

case class Subject(components: List[_ <: Component]) extends Component {

  // OPTIMIZE: Try to find better reflection. Or may be transform components to map as type name in key
  def getComponent[C <: Component : ClassTag](implicit ev: ClassTag[C]): Either[String, C] = for {
    c <- components.find(ev.runtimeClass.isInstance).toEither{"Missing component - " + ev.runtimeClass.getSimpleName}
  } yield c.asInstanceOf[C]

  def getComponents[T <: Component : ClassTag](implicit ev: ClassTag[T]): Either[String, List[T]] = ???
}