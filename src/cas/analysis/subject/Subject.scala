package cas.subject

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import components._

class Subject(components: Set[_ <: Component]) extends Component {
  val ComponentType = typeOf[Component]

  // OPTIMIZE: Try to find better reflection
  def getComponent[T: ClassTag](implicit ev: ClassTag[T]) =
    components.find(ev.runtimeClass.isInstance).map(_.asInstanceOf[T])
}