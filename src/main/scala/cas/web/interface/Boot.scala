package cas.web.interface

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import cas.analysis.estimation._
import cas.service.AProducer
import cas.utils._
import cas.web.dealers.vk.VkApiDealer
import com.typesafe.config.ConfigFactory
import spray.can.Http

import scala.concurrent.duration._
import scala.util.Try
import org.elasticsearch.common.settings.Settings

import scala.xml._
import scala.xml.factory.XMLLoader
import scala.xml.parsing.NoBindingFactoryAdapter
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import Utils._
import cas.utils.Files._
import cas.utils.UtilAliases.ErrorMsg

import scala.concurrent.duration._
import scala.xml.{Node, XML}
/*import cats.data.Validated.{invalid, valid}
import cats.std.all._
import cats.syntax.cartesian._*/
import com.textocat.textokit.commons.cpe.FileDirectoryCollectionReader
import com.textocat.textokit.commons.util.PipelineDescriptorUtils
import com.textocat.textokit.morph.dictionary.MorphDictionaryAPIFactory
import com.textocat.textokit.morph.lemmatizer.LemmatizerAPI
import com.textocat.textokit.postagger.{MorphCasUtils, PosTaggerAPI}
import com.textocat.textokit.segmentation.SentenceSplitterAPI
import com.textocat.textokit.tokenizer.TokenizerAPI
import org.apache.uima.UIMAException
import org.apache.uima.analysis_engine.AnalysisEngineDescription
import org.apache.uima.collection.CollectionReaderDescription
import org.apache.uima.fit.pipeline.SimplePipeline
import org.apache.uima.resource.ExternalResourceDescription
import java.io.File
import java.io.IOException

import com.textocat.textokit.morph.fs.Word
import org.apache.uima.fit.component.JCasAnnotator_ImplBase
import org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription
import org.apache.uima.fit.factory.AnalysisEngineFactory._
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas

object ImplicitRuntime {
  implicit val system = ActorSystem("web-service")
  val timeout = 10.seconds
}

import spray.client.pipelining._
object Boot extends App {
  import ImplicitRuntime._
  import system.dispatcher

  implicit val timeout = Timeout(10.seconds)
  val interface = system.actorOf(Props[AInterfaceControl], "interface-control")

  val config =  ConfigFactory.load()
  val addr = config.getString("cas.interface")
  val port = config.getInt("cas.port")

  println(s"Starting server on $addr:$port")

  IO(Http) ? Http.Bind(interface, addr, port)

  /*val a = (valid[String, Int](1) combine
           valid[String, Int](2) combine
           valid[String, Int](3)) map {_ + _ + _}

  println(a)*/

  /*val inputDir = new File("C:\\Users\\Albert\\Code\\Scala\\Utils")

  val readerDesc = FileDirectoryCollectionReader.createDescription(inputDir)

  val aeDesc = createEngineDescription(
    createEngineDescription(TokenizerAPI.AE_TOKENIZER),
    createEngineDescription(SentenceSplitterAPI.AE_SENTENCE_SPLITTER),
    createEngineDescription(PosTaggerAPI.AE_POSTAGGER),
    createEngineDescription(LemmatizerAPI.AE_LEMMATIZER)
  );
  val morphDictDesc =
    MorphDictionaryAPIFactory.getMorphDictionaryAPI().getResourceDescriptionForCachedInstance()
  morphDictDesc.setName(PosTaggerAPI.MORPH_DICTIONARY_RESOURCE_NAME)
  PipelineDescriptorUtils.getResourceManagerConfiguration(aeDesc)
    .addExternalResource(morphDictDesc)

  val writerDesc = createEngineDescription(WordPosLemmaWriter.getClass)

  SimplePipeline.runPipeline(readerDesc, aeDesc, writerDesc)*/

}

/*object W1 extends Word {}

object WordPosLemmaWriter extends JCasAnnotator_ImplBase {
  import scala.collection.JavaConversions._

  def process(jCas: JCas) {
    for(w <- JCasUtil.select(jCas, W1.getClass)) {
      val src: String = w.getCoveredText()
      val lemma: String = MorphCasUtils.getFirstLemma(w)
      val posTag: String = MorphCasUtils.getFirstPosTag(w)
      System.out.print(String.format("%s/%s/%s ", src, lemma, posTag))
    }
    // mark the end of a document
    System.out.println("\n")
  }
}*/
