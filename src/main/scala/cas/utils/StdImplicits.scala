package cas.utils

import cas.utils.UtilAliases.ErrorMsg

import scala.util.{Failure, Success, Try}

object StdImplicits {
  implicit class RightBiasedEither[A, B](val e: Either[A, B]) extends AnyVal {
    def foreach[U](f: B => U): Unit = e.right.foreach(f)
    def map[C](f: B => C): Either[A, C] = e.right.map(f)
    def flatMap[C](f: B => Either[A, C]) = e.right.flatMap(f)

    def get = e.right.get
    def getOrElse[BB >: B](value: => BB): BB = e.right.getOrElse(value)
  }

  implicit class TryOps[T](val t: Try[T]) extends AnyVal {
    def eventually[Ignore](effect: => Ignore): Try[T] = t.transform(_ => { effect; t }, _ => { effect; t })
    def toEither: Either[Throwable, T] = t match {
      case Success(some) => Right(some)
      case Failure(err) => Left(err)
    }
    def asEitherString: Either[ErrorMsg, T] = t match {
      case Success(some) => Right(some)
      case Failure(err) => Left(err.getMessage)
    }
  }

  implicit class OptionOps[T](val o: Option[T]) extends AnyVal {
    def toEither[L](whenLeft: => L): Either[L, T] = o match {
      case Some(s) => Right(s)
      case None => Left(whenLeft)
    }
  }
}
