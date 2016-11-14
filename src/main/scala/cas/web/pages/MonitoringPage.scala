package cas.web.pages

import akka.util.Timeout
import cas.web.dealers.vk.VkApiDealer
import spray.routing.Directives._
import spray.routing._

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import cas.web.pages.templates.Templates._

import scala.xml.XML

// TODO: Rename to vk auth
object MonitoringPage {
  import cas.service.AServiceControl.{GetStatus, Start, Status, Stop}
  import cas.web.interface.ImplicitRuntime._
  import system.dispatcher
  implicit val timeout = Timeout(60.seconds)
  val deletingCommentsAmt = 500
  val lastDeletedComments = scala.collection.mutable.Queue[String]()

  /*new Period().plusSeconds(10) ->  1.0)*/

  def addDeletedComment(c: String) = {
    if (lastDeletedComments.length> deletingCommentsAmt) {
      lastDeletedComments.dequeue()
    }
    lastDeletedComments.enqueue(c)
  }

	def apply(pagePath: String) = path(pagePath) {
      get {
        complete(getHtml(lastDeletedComments))
      }
	}

  def getHtml(delComments: mutable.Queue[String]) = defaultTemplate { <div>
      <h3>Last {deletingCommentsAmt} deleted comments:</h3>
      {for {
        c <- delComments.reverse
      } yield <div class="mb20 divider-top">{ c }</div> } </div>
  }
}