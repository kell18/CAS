package cas.utils

object Extractors {

  class Extractor[T] {
    def unapply(a: Any): Option[T] = Some(a.asInstanceOf[T])
  }

  object Dict extends Extractor[Map[String, Any]]

  object Lst extends Extractor[List[Any]]

  object Str extends Extractor[String]

  object Dbl extends Extractor[Double]

  object Lng extends Extractor[Long]

  object Bool extends Extractor[Boolean]

}