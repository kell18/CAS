package cas.model.subject

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import components._


class Subject(components: List[_ <: Component]) extends Component("Subject") {
  val ComponentType = typeOf[Component]

  // TODO: Cache typeOf[T]
  def getComponent[T: TypeTag] = components.find(c => typeOf[T] <:< ComponentType).map(_.asInstanceOf[T])
}

trait ActualityEstimator {
  def estimateActuality(subj: Subject)
}

class LoyalityEstimator(val likesThresh: Double, val repostsThresh: Double) extends ActualityEstimator()
{
  override def estimateActuality(subj: Subject) = for {
    likeab <- subj.getComponent[Likeability]
    viral <- subj.getComponent[Virality]
  } yield estimateLoyality(likeab.value, viral.value)

  def estimateLoyality(likes: Double, reposts: Double) = 
    if (likes >= likesThresh || reposts >= repostsThresh) 1.0 else 0.0
}
