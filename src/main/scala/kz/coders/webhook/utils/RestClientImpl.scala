package kz.coders.webhook.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.Materializer
import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, XML}

object RestClientImpl {

  def parseResponse(
                     response: HttpResponse
                   )(implicit materializer: Materializer, ex: ExecutionContext): Future[String] =
    response.entity.toStrict(10.seconds).flatMap { entity =>
      entity.dataBytes
        .runFold(ByteString.empty)((acc, bytes) => acc ++ bytes)
        .map(_.utf8String)
    }

  def getXml(url: String): Elem = {
    val xml = XML.load(url)
    xml
  }

  def get(url: String,
          headers: List[RawHeader],
          query: Option[Map[String, String]] = None)(
           implicit system: ActorSystem,
           materializer: Materializer,
           ex: ExecutionContext
         ): Future[String] = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = url,
      headers = headers
    )
    Http().singleRequest(request).flatMap(parseResponse)
  }

  def post[T](url: String,
              data: T,
              query: Option[Map[String, String]] = None,
              headers: Option[Map[String, String]] = None)(
               implicit system: ActorSystem,
               materializer: Materializer,
               ex: ExecutionContext
             ): Future[String] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = url,
      entity =
        HttpEntity(contentType = ContentTypes.`application/json`, data.toString)
    )
    Http().singleRequest(request).flatMap(parseResponse)
  }

}
