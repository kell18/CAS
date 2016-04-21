package cas.utils

import cas.persistence.searching.ElasticSearch
import org.joda.time.DateTime

object RemoteLogger {
  import cas.web.interface.ImplicitRuntime.system
  import system.dispatcher

  private val logger = new ElasticSearch(index = "cas-log", mtype = "lines")
  val initF = logger.initStorage

  def info(msg: String) = initF.flatMap { _ => logger.pushEntity(getTime + msg) }

  def getTime = {
    val now = DateTime.now()
    "[" + now.dayOfMonth().get() + " - " + now.hourOfDay().get() + ":" + now.minuteOfHour().get() + "] "
  }
}

