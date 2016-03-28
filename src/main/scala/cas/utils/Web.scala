package cas.utils

object Web {
  def buildRequest(url: String)(page: String)(params: List[(String, String)]) = url + page + (params match {
    case Nil => ""
    case p::ps => "?" + ps.foldLeft(p._1 + "=" + p._2) { (l, r) => l + "&" + r._1 + "=" + r._2 }
  })
}
