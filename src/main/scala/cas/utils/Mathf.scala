package cas.utils

import math.{min, max}
import Ordering.Implicits._

object Mathf {
  val sec2Millis = 1000

  def clamp[T: Ordering](a: T, lower: T, upper: T) = if (a < lower) lower else if (a > upper) upper else a
}
