package cas.web.pages

import cas.utils.Extractors._
import cas.utils.{Files, Web}
import cas.web.dealers.vk.VkApiProtocol.VkApiConfigs
import cas.web.dealers.vk.VkApiDealer
import cas.web.model.UsingDealer
import cas.web.model.UsingDealerProtocol._
import spray.client.pipelining._
import spray.routing.Directives._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal
import spray.json._

object VkAuth {
  import VkApiDealer._
  import cas.web.interface.ImplicitRuntime._
  import system.dispatcher

  val persistPath = Files.dealers + "/" + VkApiDealer.id
  val domain = "http://localhost:8080/"
  val pipeline = sendReceive

	def apply(pagePath: String) = path(pagePath) {
    get {
      parameter("code".?, "owner_id".as[Long].?, "sifting_posts".as[Int].?) { (code, ownerId, sCount) =>
        val fileConfigs = Files.readFile(persistPath)
                                .map(_.parseJson.convertTo[VkApiConfigs])
                                .recover { case NonFatal(ex) => VkApiConfigs(null, 0, 0) }
        val token = if (code.isDefined) Try(Await.result(pullToken(code.get, domain + pagePath), 10.seconds))
                    else fileConfigs.flatMap(c => if (c.token != null) Success(c.token)
                                                  else Failure(new Exception("No token")))
        val resultConfigs = for {
          t <- token
          c <- fileConfigs
        } yield VkApiConfigs(t, ownerId.getOrElse(c.ownerId), sCount.getOrElse(c.siftCount))
        resultConfigs.foreach(c => {
          Files.writeToFile(Files.currentDealer, UsingDealer(VkApiDealer.id).toJson.prettyPrint)
          Files.writeToFile(persistPath, c.toJson.prettyPrint)
        })
        complete {
          <html>
            <body>
              <h4>Vk API authentication</h4>
              {if (code.isEmpty) <a href={authUrl(pagePath)}>Auth in VK</a>
               else if (token.isSuccess) <span>Auth succeeded. <a href={authUrl(pagePath)}>Re-auth in VK</a></span>
                    else {
                      println("Vk auth failed: " + token.failed.get.getMessage)
                      <span>Auth failed, try again.</span>
                    }}
              <br/><br/>
              { if (token.isSuccess) {
                <span>Account authorized.</span><br/><br/>
                <form method="GET" action={pagePath}>
                  <label>Owner ID</label>
                  <input type="number" name="owner_id" value={resultConfigs.map(_.ownerId).getOrElse(0).toString} />
                  <label>Count of posts to filter</label>
                  <input type="number" name="sifting_posts" value={resultConfigs.map(_.siftCount).getOrElse(0).toString} />
                  <input type="submit" value="Update settings" />
                </form>
              }}
            </body>
          </html>
        }
      }
    }
  }

  /*(domain + returnToPage)*/
  def authUrl(returnToPage: String) = Web.buildRequest("https://oauth.vk.com/")("authorize")(
    "redirect_uri" -> "https://oauth.vk.com/blank.html":: "client_id" -> clientId :: "scope" -> scope :: "display" -> "popup" ::
    "response_type" -> "token" :: "revoke" -> "1" :: "v" -> apiVersion :: Nil)

  def pullToken(code: String, path: String) = for {
    resp <- pipeline(Get(Web.buildRequest("https://oauth.vk.com/")("access_token")("client_id" -> clientId ::
      "client_secret" -> clientSecret :: "redirect_uri" -> path :: "code" -> code :: "v" -> apiVersion :: Nil)))
    token = resp.entity.asString.parseJson.asJsObject.fields("access_token").convertTo[String]
  } yield token

}