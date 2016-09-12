package cas.math

import scala.Ordering.Implicits._

object Mathf {
  val sec2Millis = 1000

  def sigmoid(x: Double) = 1.0 / (1.0 + math.exp(-x))

  def clamp[T: Ordering](a: T, lower: T, upper: T) = if (a < lower) lower else if (a > upper) upper else a

  def F1Measure(TP: Double, TN: Double, FP: Double, FN: Double, alpha: Double = 1) = {
    val P = Precision(TP, FP)
    val R = Recall(TP, FN)
    (2.0 * P * R) / (P + R)
  }

  def Precision(TP: Double, FP: Double) = TP / (FP + TP)

  def Recall(TP: Double, FN: Double) = TP / (FN + TP)
}
