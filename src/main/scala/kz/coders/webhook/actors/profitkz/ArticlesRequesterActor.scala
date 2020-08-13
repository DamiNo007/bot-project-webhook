package kz.coders.webhook.actors.profitkz

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.stream.Materializer
import cats.syntax.all._
import com.lucidchart.open.xtract.XmlReader.{seq, _}
import com.lucidchart.open.xtract.{XmlReader, __}
import com.typesafe.config.Config
import kz.coders.webhook.actors.ReceivedResponse
import kz.coders.webhook.actors.profitkz.ArticlesRequesterActor._
import org.json4s.{DefaultFormats, Formats}
import kz.coders.webhook.utils.RestClientImpl._
import scala.concurrent.ExecutionContext

object ArticlesRequesterActor {

  case class Articles(
                       items: Seq[ArticleItem]
                     )

  case class ArticleItem(
                          title: String,
                          description: String,
                          link: String
                        )

  object ArticleItem {
    implicit val reader: XmlReader[ArticleItem] = (
      (__ \ "title").read[String],
      (__ \ "description").read[String],
      (__ \ "link").read[String]
      ).mapN(apply _)
  }

  object Articles {
    implicit val reader: XmlReader[Articles] = (
      (__ \ "channel" \ "item").read(seq[ArticleItem])
      ).map(apply _)
  }

  case class GetArticlesAll(msg: String)

}

class ArticlesRequesterActor(config: Config)(implicit val system: ActorSystem,
                                             val materializer: Materializer)
  extends Actor with ActorLogging {

  implicit val ex: ExecutionContext = context.dispatcher
  implicit val formats: Formats = DefaultFormats
  val baseUrl = config.getString("profitKZ.base-url")

  def mkListString(list: List[ArticleItem]): List[String] = {
    list.zipWithIndex.map {
      case (ArticleItem(title, description, link), id) =>
        s"""
           |${id + 1}. Title: $title
           |Description: $description
           |Link: $link""".stripMargin
    }
  }

  def getArticles: List[ArticleItem] = {
    val xml = getXml(s"$baseUrl/articles")
    val parseRes = XmlReader.of[Articles].read(xml).getOrElse("unknown")
    val items = parseRes.asInstanceOf[Articles].items.toList
    items
  }

  override def receive: Receive = {
    case GetArticlesAll(msg) =>
      log.info(s"got message $msg")
      val sender = context.sender()
      val items = getArticles
      val result = mkListString(items.take(5))
      result match {
        case head :: tail =>
          sender ! ReceivedResponse(result.mkString("\n"))
        case _ =>
          sender ! ReceivedResponse("No articles found")
      }
  }
}

