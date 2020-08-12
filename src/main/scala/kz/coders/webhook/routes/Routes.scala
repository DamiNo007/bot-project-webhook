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

  def extractResponse(response: Future[Any]): Future[WebhookResponse] = {
    response
      .mapTo[Response]
      .map {
        case res: ReceivedResponse =>
          WebhookResponse(
            Array(
              WebhookResponseText(
                WebhookInnerText(
                  Array(res.response)
                )
              )
            )
          )
        case res: ReceivedFailureResponse =>
          WebhookResponse(
            Array(
              WebhookResponseText(
                WebhookInnerText(
                  Array(res.error)
                )
              )
            )
          )
      }
  }

  def processRequest(request: WebhookRequest): Future[WebhookResponse] = {
    request.queryResult.intent.displayName match {
      case "get-github-account" =>
        val login = request.queryResult.parameters("github-account")
        extractResponse(
          githubWorkerActor ? GetUser(login)
        )
      case "get-github-repos" =>
        val login = request.queryResult.parameters("github-repos")
        extractResponse(
          githubWorkerActor ? GetRepositories(login)
        )
      case "get-currencies" =>
        val params = request.queryResult.parameters("currencies")
        extractResponse(exchangeWorkerActor ? GetCurrencies(params))
      case "get-convert" =>
        val amount = request.queryResult.parameters("amount")
        val from = request.queryResult.parameters("from")
        val to = request.queryResult.parameters("to")
        extractResponse(
          exchangeWorkerActor ? Convert(from, to, amount)
        )
      case "get-rates" =>
        val currency = request.queryResult.parameters("rates")
        extractResponse(
          exchangeWorkerActor ? GetRates(currency)
        )
      case "get-news" =>
        val params = request.queryResult.parameters("news")
        extractResponse(
          newsWorkerActor ? GetNews(params)
        )
      case "get-articles" =>
        val params = request.queryResult.parameters("articles")
        extractResponse(
          articlesWorkerActor ? GetArticles(params)
        )
    }
  }

  val handlers: Route = pathPrefix("api") {
    pathPrefix("bot") {
      pathPrefix("webhook") {
        post {
          entity(as[WebhookRequest]) { body =>
            ctx =>
              val response = processRequest(body)
              ctx.complete(response)
          }
        }
      }
    }
  }
}