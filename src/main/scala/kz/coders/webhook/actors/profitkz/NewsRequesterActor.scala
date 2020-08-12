package kz.coders.webhook.actors.profitkz

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.stream.Materializer
import cats.syntax.all._
import com.lucidchart.open.xtract.XmlReader.{seq, _}
import com.lucidchart.open.xtract.{XmlReader, __}
import com.typesafe.config.Config
import kz.coders.webhook.actors.ReceivedResponse
import kz.coders.webhook.actors.profitkz.NewsRequesterActor._
import org.json4s.{DefaultFormats, Formats}
import kz.coders.webhook.utils.RestClientImpl._
import scala.concurrent.ExecutionContext

object NewsRequesterActor {

  case class News(
                   items: Seq[NewsItem]
                 )

  case class NewsItem(
                       title: String,
                       description: String,
                       link: String
                     )

  object NewsItem {
    implicit val reader: XmlReader[NewsItem] = (
      (__ \ "title").read[String],
      (__ \ "description").read[String],
      (__ \ "link").read[String]
      ).mapN(apply _)
  }

  object News {
    implicit val reader: XmlReader[News] = (
      (__ \ "channel" \ "item").read(seq[NewsItem])
      ).map(apply _)
  }

  case class GetNewsAll(msg: String)

  case class GetNewsAllHttp(msg: String)

}

class NewsRequesterActor(config: Config)(implicit val system: ActorSystem,
                                         val materializer: Materializer)
  extends Actor with ActorLogging {

  implicit val ex: ExecutionContext = context.dispatcher
  implicit val formats: Formats = DefaultFormats
  val baseUrl = config.getString("profitKZ.base-url")

  def mkListString(list: List[NewsItem]): List[String] = {
    list.zipWithIndex.map {
      case (NewsItem(title, description, link), id) =>
        s"""
           |${id + 1}. Title: $title
           |Description: $description
           |Link: $link""".stripMargin
    }
  }

  def getNews: List[NewsItem] = {
    val xml = getXml(s"$baseUrl/news")
    val parseRes = XmlReader.of[News].read(xml).getOrElse("unknown")
    val items = parseRes.asInstanceOf[News].items.toList
    items
  }

  override def receive: Receive = {
    case GetNewsAll(msg) =>
      log.info(s"got message $msg")
      val sender = context.sender()
      val items = getNews
      val result = mkListString(items.take(5))
      result match {
        case head :: tail =>
          sender ! ReceivedResponse(result.mkString("\n"))
        case _ =>
          sender ! ReceivedResponse("No news found")
      }
  }
}

