package cas.persistence

import scala.collection.mutable
import akka.event.LoggingAdapter
import cas.analysis.subject._
import cas.analysis.subject.components.{CreationDate, _}
import cas.utils.Files
import cas.web.pages.ConfigurePage
import spray.json._
import cas.utils.StdImplicits._
import cas.utils.Utils.escapeJson

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import cas.web.interface.ImplicitRuntime.timeout
import org.elasticsearch.common.recycler.Recycler.V
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.collection.parallel.immutable
import scala.collection.parallel.immutable._

object SubjectsGraderProtocol extends DefaultJsonProtocol {
  case class Data(text: String, likes: Int, relevance: Double, hourOfDay: Int, isActual: Int = 1)
  val dataFormat = jsonFormat5(Data)

  case class Data1(text: String, likes: Int, relevance: Double, date: String, post: String, isActual: Int = 1)
  val data1Format = jsonFormat6(Data1)

  implicit def mapStringFormat = new RootJsonFormat[Map[String, String]] {
    def write(m: Map[String, String]) = JsObject {
      m.map { field =>
        field._1.toJson match {
          case JsString(x) => x -> field._2.toJson
          case x => throw new SerializationException("Map key must be formatted as JsString, not '" + x + "'")
        }
      }
    }
    def read(value: JsValue) = value match {
      case x: JsObject => x.fields.map { field =>
        (field._1, field._2.toString())
      } (collection.breakOut)
      case x => deserializationError("Expected Map as JsObject, but got " + x)
    }
  }

  implicit def mapDataFormat = new RootJsonFormat[Map[String, Data]] {
    def write(m: Map[String, Data]) = JsObject {
      m.map { field =>
        field._1.toJson match {
          case JsString(x) => x -> field._2.toJson(dataFormat)
          case x => throw new SerializationException("Map key must be formatted as JsString, not '" + x + "'")
        }
      }
    }
    def read(value: JsValue) = value match {
      case x: JsObject => x.fields.map { field =>
        (field._1, field._2.convertTo[Data](dataFormat))
      } (collection.breakOut)
      case x => deserializationError("Expected Map as JsObject, but got " + x)
    }
  }

  implicit def mapData1Format = new RootJsonFormat[Map[String, Data1]] {
    def write(m: Map[String, Data1]) = JsObject {
      m.map { field =>
        field._1.toJson match {
          case JsString(x) => x -> field._2.toJson(data1Format)
          case x => throw new SerializationException("Map key must be formatted as JsString, not '" + x + "'")
        }
      }
    }
    def read(value: JsValue) = value match {
      case x: JsObject => x.fields.map { field =>
        (field._1, field._2.convertTo[Data1](data1Format))
      } (collection.breakOut)
      case x => deserializationError("Expected Map as JsObject, but got " + x)
    }
  }

  case class Snapshot(data: Map[String, Data1], context: Map[String, String])
  val snapshotFormat = jsonFormat2(Snapshot) // TODO: Вручную
}

class SubjectsGrader(val pushThreshold: Int = 50)(implicit log: LoggingAdapter) {
  import cas.persistence.SubjectsGraderProtocol._
  val searcher = ConfigurePage.searcher

  val data: mutable.Map[String, Data1] = mutable.Map()
  val context: mutable.Map[String, String] = mutable.Map()

  val dateFormatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");

  def snapshotPath =
    s"${Files.resources}/cas/data/snapshot_from-${new DateTime().toString("m-k-d-M")}_amt-${data.size}.json"

  def commitSubject(subj: Subject) = for {
    id <- subj.getComponent[ID]
    descr <- subj.getComponent[Description]
    errOrRelevance <- Try(Await.result(searcher.queryEntityScore(descr.text), timeout)).asEitherString
    relevance <- errOrRelevance
    date <- subj.getComponent[CreationDate]
    likes <- subj.getComponent[Likability]
    article <- subj.getComponent[Article]
  } yield {
    if (!data.contains(id.value)) println("data.size: " + data.size)
    data(id.value) = Data1(escapeJson(descr.text), likes.value.toInt,
      relevance.maxScore.getOrElse(0.0), date.value.toString(dateFormatter), article.brief)
    if (!context.contains(article.id)) context += article.id -> escapeJson(article.body)
    if (data.size >= pushThreshold) pushToFile
  }

  def pushToFile = {
    val snapshot = Snapshot(data.toMap, context.toMap)
    for {
      s <- Files.writeToFile(snapshotPath, snapshot.toJson(snapshotFormat).prettyPrint).asEitherString
    } yield {
      data.clear()
      context.clear()
    }
  }


}
