package cas.subject

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scalaz._
import scalaz.Scalaz._
import cas.subject.components._

// TODO(1): Make case
class Subject(components: Set[_ <: Component]) extends Component {
  val ComponentType = typeOf[Component]

  // OPTIMIZE: Try to find better reflection
  def getComponent[T <: Component : ClassTag](implicit ev: ClassTag[T]): Either[String, T] =
    components.find(ev.runtimeClass.isInstance).map(_.asInstanceOf[T]).toRight(ev.runtimeClass.getSimpleName)

}