package cas.web.pages

import cas.web.interface.PageUrls
import cas.web.pages.templates.Templates._
import spray.http.{HttpCookie, StatusCodes}
import spray.routing.Directives._
import scala.collection.Searching.Found

object LoginPage {

  def apply(pageRelPath: String, redirectOnSuccesPath: String) = path(pageRelPath) {
    content(pageRelPath, redirectOnSuccesPath)
  }

  def content(pageRelPath: String, redirectOnSuccesPath: String) = get {
    parameters("login".as[String].?, "passw".as[String].?) { (loginOpt, passwOpt) =>
      optionalCookie("cas_token") {
        case Some(casToken) => complete(getHtml(pageRelPath, isLoggedIn = true))
        case _ =>
          if (loginOpt.isDefined && passwOpt.isDefined)
            if (isCorrectLoginPassw(loginOpt, passwOpt)) {
              spray.routing.Directives.setCookie(HttpCookie("cas_token", tokens.head)) {
                redirect(redirectOnSuccesPath, StatusCodes.TemporaryRedirect)
              }
            }
            else
              complete(getHtml(pageRelPath, isLoggedIn = false, isFailedLoginTry = true))
          else
            complete(getHtml(pageRelPath, isLoggedIn = false))
      }
    }
  }

  def getHtml(pagePath: String, isLoggedIn: Boolean, isFailedLoginTry: Boolean = false) = defaultTemplate {
    <div>
      {if (isLoggedIn) {
      <h3>Logged in</h3>
      <form method="GET" action={pagePath}>
        <input onclick="document.cookie = 'cas_token=; expires=Thu, 01 Jan 1970 00:00:01 GMT;'"
               type="submit" value="Log-out" />
      </form>
    } else {
      if (isFailedLoginTry) {
        <p class="error">Wrong login or password.</p>
      }
      <h3>Log in:</h3>
        <form method="GET" action={pagePath}>
          <label>Login:</label>
          <input type="text" name="login"></input>
          <label>Password:</label>
          <input type="password" name="passw"></input>
          <p>
            <input type="submit" value="Login"/>
          </p>
        </form>
    }}
    </div>
  }


  def isCorrectLoginPassw(login: Option[String], passw: Option[String]) : Boolean = (for {
    l <- login
    p <- passw
    if accounts.exists(logPasw => logPasw == l -> p)
  } yield { true }).isDefined

  val tokens = List[String]("c7e7cd4c27e60c0650ac38104f78ce83")
  val accounts = Map[String, String]("ln_admin" -> "123") // t3aKtG57
}