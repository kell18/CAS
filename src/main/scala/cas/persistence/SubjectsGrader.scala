package cas.persistence

import akka.actor.{Actor, ActorLogging}
import akka.actor.Actor.Receive

import scala.collection.mutable
import akka.event.LoggingAdapter
import cas.analysis.subject._
import cas.analysis.subject.components.{CreationDate, _}
import cas.persistence.SubjectsGraderProtocol.Snapshot
import cas.service.ARouter.Estimation
import cas.service.ContentDealer
import cas.utils.{Files, Regexf}
import cas.web.pages.ConfigurePage
import spray.json._
import cas.utils.StdImplicits._
import cas.utils.Utils.escapeJson

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.reflect.classTag
import scala.util.{Failure, Success, Try}
import cas.web.interface.ImplicitRuntime.timeout
import org.elasticsearch.common.recycler.Recycler.V
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.collection.parallel.immutable._

object SubjectsGraderProtocol extends DefaultJsonProtocol {
  import spray.json.CollectionFormats

  val dateFormatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss")

  case class Dump(id: String, text: String, likes: Int, relevance: Double,
                  date: String, post: String, isToDelete: Int = 0) {
    //    def toTsv
  }
  implicit val dumpFormat = jsonFormat7(Dump)

  case class Features(text: String, wordsAmt: Int, likes: Int, relevance: Double, date: String, post: String)
  implicit val featuresFormat = jsonFormat6(Features)

  case class Data(id: String, features: Features, isToDelete: Int = 0) {
    def toCsv = features.wordsAmt + "," + features.likes + "," +
      features.relevance + "," + dateFormatter.parseDateTime(features.date).getHourOfDay

    def toOctave = features.wordsAmt + " " + features.likes + " " + features.relevance
  }
  implicit val dataFormat = jsonFormat3(Data)(StringJsonFormat, featuresFormat, IntJsonFormat, classTag[Data])

  case class Snapshot(data: List[Dump])
  implicit val snapshotFormat = jsonFormat1(Snapshot)

  case class OldDump(text: String, likes: Int, relevance: Double, hourOfDay: Int, isActual: Int = 1)
  implicit val oldDumpFormat = jsonFormat5(OldDump)

  case class SnapshotOld(data: Map[String, OldDump], context: Map[String, String])
  implicit val snapshotOldFormat = jsonFormat2(SnapshotOld)
}

class SubjectsGrader(val pushThreshold: Int = 50) {
  import spray.json.CollectionFormats
  import cas.persistence.SubjectsGraderProtocol._

  val searcher = ConfigurePage.searcher
  val data: mutable.Map[String, Dump] = mutable.Map()

  def commitSubject(subj: Subject) = for {
    subjId <- subj.getComponent[ID]
    descr <- subj.getComponent[Description]
    errOrRelevance <- Try(Await.result(searcher.queryEntityScore(descr.text), timeout)).asEitherString
    relevance <- errOrRelevance
    date <- subj.getComponent[CreationDate]
    likes <- subj.getComponent[Likability]
    article <- subj.getComponent[Article]
  } yield {
    val id = article.id + "_" + subjId.value
    if (!data.contains(id)) println("data.size: " + (data.size + 1))
    data(id) = Dump(id, escapeJson(descr.text), likes.value.toInt,
      relevance.maxScore.getOrElse(0.0), date.value.toString(dateFormatter), article.brief)
    if (data.size >= pushThreshold) pushToFile(Snapshot(data.values.toList))
  }

  def pushToFile(snapshot: Snapshot, pathOpt: Option[String] = None) = {
    val path = if (pathOpt.isEmpty) snapshotPath else pathOpt.get
    for {
      s <- Files.writeToFile(path, snapshot.toJson(snapshotFormat).prettyPrint).asEitherString
    } yield {
      data.clear()
    }
  }

  def pushToFile(snapshot: List[Data]) = {
    for {
      s <- Files.writeToFile(snapshotPath, snapshot.toJson.prettyPrint).asEitherString
    } yield {
      data.clear()
    }
  }

  def snapshotPath =
    s"${Files.resources}/cas/data/raw/snapshot_from-${new DateTime().toString("k-d-M")}_amt-${data.size}.json"

  def convertDumpToData(pathToRaw: String, newPathOpt: Option[String] = None) = for {
    strSnap <- Files.readFile(pathToRaw)
    rawSnap <- Try(strSnap.parseJson.convertTo[Snapshot])
    dataList = for {
      datItm <- rawSnap.data
    } yield {
      val wordsCnt = Regexf.cleanAndSplit(datItm.text).length
      val ftrs = Features(datItm.text, wordsCnt, datItm.likes, datItm.relevance, datItm.date, datItm.post)
      Data(datItm.id, ftrs, datItm.isToDelete)
    }
    result <- if (newPathOpt.isDefined) Files.writeToFile(newPathOpt.get, dataList.toJson.prettyPrint) else Success(())
  } yield dataList

  def convertDumpToEstimations(pathToRaw: String) = for {
    strSnap <- Files.readFile(pathToRaw)
    rawSnap <- Try(strSnap.parseJson.convertTo[Snapshot])
    subjsList = for { data <- rawSnap.data } yield {
      Estimation(Subject(List(
        ID(data.id),
        Description(data.text),
        Likability(data.likes.toDouble),
        Relevance(data.relevance),
        CreationDate(dateFormatter.parseDateTime(data.date)),
        Article("", data.post, data.post) // TODO: Care
      )), (1 - data.isToDelete).toDouble)
    }
  } yield subjsList

  def convertOldDumpToNew(pathToOld: String, startDate: DateTime, newPath: String) = for {
    file <- Files.readFile(pathToOld)
    oldSnap <- Try(file.parseJson.convertTo[SnapshotOld])
    snapshot = Snapshot(for {
      (id, datItm) <- oldSnap.data.toList
    } yield {
      val date = startDate.plusHours(datItm.hourOfDay)
      Dump(id, datItm.text, datItm.likes, datItm.relevance,
        date.toString(dateFormatter), "", if (datItm.isActual > 0) 0 else 1)
    })
    result <- Files.writeToFile(newPath, snapshot.toJson.prettyPrint)
  } yield result
}

class ASubjectsGrader(dealer: ContentDealer, pushThreshold: Int) extends Actor with ActorLogging {
  import context.dispatcher

  case object QueryTick

  val grader = new SubjectsGrader(pushThreshold)

  override def preStart(): Unit = {
    super.preStart()
    context.system.scheduler.schedule(15.second, 15.second, context.self, QueryTick)
  }

  override def receive: Receive = {
    case QueryTick =>
      dealer.pullSubjectsChunk onComplete {
        case Success(Right(subjs)) => subjs.foreach(grader.commitSubject)
        case error => log.error("Cannot pull subjects: `" + error + "`")
      }
  }

  override def postStop(): Unit = {
    super.postStop()
    if (grader.data.size > 10) grader.pushToFile(Snapshot(grader.data.values.toList))
  }
}