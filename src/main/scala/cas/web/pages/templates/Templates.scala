package cas.web.pages.templates

import java.net.{URLDecoder, URLEncoder}
import javax.xml.bind.annotation.XmlElement
import cas.utils.Files
import cas.utils.Files._
import scala.io.Source
import scala.xml.{Elem, XML}
import scala.xml.pull.XMLEventReader

object Templates {

	// TODO: Separate view and model
	val defaultTemplate = page("/static/head.html")(<a href="/control">Control</a> ::
																								<a href="/monitoring">Monitoring</a> ::
																								<a href="/auth">Auth</a> :: Nil) (_)

	def page(pathToStatic: String)(menuElements: List[Elem])(content: Elem) = <html>
		<head> { XML.load(getClass.getResourceAsStream(pathToStatic)) } </head>
		<body>
			<h2>Content Analysis System</h2>
			{ headerMenu(menuElements) }
			{ centered(<div class="center mb40"> { content } </div>) }
			{ footer(<span> 2016, Content Analysis System </span>) }
		</body>
	</html>

	def headerMenu(elements: List[Elem]) = <div class="menu">
		{ for (el <- elements) yield <span class="menu-item"> { el } </span> }
	</div>

	def centered(content: Elem) = <div class="allign-center"> { content } </div>

	def footer(content: Elem) = <div class="footer"> { content } </div>
}
