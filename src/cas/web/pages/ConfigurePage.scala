package cas.web.pages

import spray.routing._
import Directives._
import cas.utils.Extractors._
import cas.utils.Web
import cas.web.dealers.VkConfigs
import spray.client.pipelining._
import scala.concurrent.Future
import scala.util.parsing.json.JSON

// TODO: Rename to vk auth
object ConfigurePage {
  import VkConfigs._
  val pipeline = sendReceive
  val domain = "http://localhost/"

	def apply(pagePath: String) = path(pagePath){
		get	{
			parameter("code".?, "error".?) { (code, error) =>
        val redirectUrl = Web.buildRequest("https://oauth.vk.com/")("authorize")("redirect_uri" -> (domain + pagePath) ::
          "client_id" -> clientId :: "scope" -> scope :: "display" -> "popup" ::
          "response_type" -> "code" :: "v" -> apiVersion :: Nil)
        println("!!! redirectUrl: " + redirectUrl)
        if (code.isDefined)
				complete {
          <html>
            <body>
              <h2>Content Analysis System</h2>
              <p>{ if (code.isEmpty) <a href={redirectUrl}>Authorize</a> else <span>Authorization succeeded.</span> }</p>
              <p>{ if (error.isDefined) <b> Authorization failed: {error.get}. Try to reload page.</b> }</p>
            </body>
          </html>
				}
			}
		}
	}

  def pullCode(path: String): Future[String] = {
    for {
      resp <- pipeline(Get())
      token <- for {
        Some(Dict(respJson)) <- List(JSON.parseFull(resp.entity.asString))
        Str(token) = respJson("access_token")
      } yield token
    } yield token
  }
}