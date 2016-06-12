package cas.math

object Optimization {
  def findMin_brute[T](func: T => Double, argsRange: Iterable[T]) = {
    argsRange.map{arg => println((arg, func(arg))); (arg, func(arg))}.minBy(argAndVal => argAndVal._2)
  }
}
