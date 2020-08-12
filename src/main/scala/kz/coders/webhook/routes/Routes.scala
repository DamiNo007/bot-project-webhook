package kz.coders.webhook.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import kz.coders.webhook.actors._
import kz.coders.webhook.routes.Routes._
import org.json4s.{DefaultFormats, Serialization}
import org.json4s.jackson.Serialization
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

object Routes {

  case class QueryResult(queryText: String,
                         parameters: Map[String, String],
                         intent: WebhookIntent)

  case class WebhookIntent(displayName: String)

  case class WebhookRequest(queryResult: QueryResult)

  case class WebhookInnerText(text: Array[String])

  case class WebhookResponseText(text: WebhookInnerText)

  case class WebhookResponse(fulfillmentMessages: Array[WebhookResponseText])

}

class Routes(logger: LoggingAdapter,
             githubWorkerActor: ActorRef,
             exchangeWorkerActor: ActorRef,
             newsWorkerActor: ActorRef,
             articlesWorkerActor: ActorRef)
            (implicit system: ActorSystem,
             materializer: Materializer,
             ex: ExecutionContext)
  extends Json4sSupport {

  implicit val formats = DefaultFormats
  implicit val serialization: Serialization = Serialization
  implicit val timeout: Timeout = 20.seconds

  def extractResponse(response: Future[Any]): Future[String] = {
    response.mapTo[Response].map {
      case res: ReceivedResponse =>
        res.response
      case res: ReceivedFailureResponse =>
        res.error
    }
  }

  def generateResponse(text: String): WebhookResponse = {
    WebhookResponse(Array(WebhookResponseText(WebhookInnerText(Array(text)))))
  }

  val handlers: Route = pathPrefix("api") {
    pathPrefix("bot") {
      pathPrefix("webhook") {
        post {
          entity(as[WebhookRequest]) { body =>
            ctx =>
              logger.info(s"received ${body}")
              body.queryResult.intent.displayName match {
                case "get-github-account" =>
                  val login = body.queryResult.parameters("github-account")
                  val response =
                    extractResponse(githubWorkerActor ? GetUser(login))
                  ctx.complete(response)
                case "get-github-repos" =>
                  val login = body.queryResult.parameters("github-repos")
                  val response =
                    extractResponse(githubWorkerActor ? GetRepositories(login))
                  ctx.complete(response)
                case "get-currencies" =>
                  val params = body.queryResult.parameters("currencies")
                  val response = extractResponse(exchangeWorkerActor ? GetCurrencies(params))
                  ctx.complete(response)
                case "get-convert" =>
                  val amount = body.queryResult.parameters("amount")
                  val from = body.queryResult.parameters("from")
                  val to = body.queryResult.parameters("to")
                  val response = extractResponse(exchangeWorkerActor ? Convert(from, to, amount))
                  ctx.complete(response)
                case "get-rates" =>
                  val currency = body.queryResult.parameters("rates")
                  val response = extractResponse(exchangeWorkerActor ? GetRates(currency))
                  ctx.complete(response)
                case "get-news" =>
                  val params = body.queryResult.parameters("news")
                  val response = extractResponse(newsWorkerActor ? GetNews(params))
                  ctx.complete(response)
                case "get-articles" =>
                  val params = body.queryResult.parameters("articles")
                  val response = extractResponse(articlesWorkerActor ? GetArticles(params))
                  ctx.complete(response)
                case _ =>
                  ctx.complete(generateResponse("Извините! Не понял. Не могли бы вы повторить."))
              }
          }
        }
      }
    }
  }
}
