package cas.utils

object Web {
  def buildRequest(url: String)(method: String)(params: List[Pair[String, String]]) = url + method + (params match {
    case Nil => ""
    case p::ps => "?" + ps.foldLeft(p._1 + "=" + p._2) { (l, r) => l + "&" + r._1 + "=" + r._2 }
  })
}
