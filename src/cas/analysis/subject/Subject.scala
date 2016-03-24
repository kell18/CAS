package cas.subject

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scalaz._
import scalaz.Scalaz._
import cas.subject.components._

case class Subject(components: List[_ <: Component]) extends Component {
  val ComponentType = typeOf[Component]

  // OPTIMIZE: Try to find better reflection. Or may be transform components to map as type name in key
  def getComponent[T <: Component : ClassTag](implicit ev: ClassTag[T]): Either[String, T] =
    components.find(ev.runtimeClass.isInstance).map(_.asInstanceOf[T]).toRight(ev.runtimeClass.getSimpleName)

  def getComponents[T <: Component : ClassTag](implicit ev: ClassTag[T]): Either[String, List[T]] = ???
}