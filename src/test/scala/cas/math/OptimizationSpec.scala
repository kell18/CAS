package cas.math

import org.specs2.mutable.Specification

import scala.collection.immutable.NumericRange

class OptimizationSpec extends Specification  {

  "Optimization" should {
    "brute minimize func" in {
      val func = (x: Int) => (x * x).toDouble + 1.0
      val args = NumericRange(-3, 3, 1)
      val minVal = (0, 1.0)
      val foundMinVal = Optimization.findMin_brute(func, args)
      minVal must beEqualTo(foundMinVal)
    }
  }
}
