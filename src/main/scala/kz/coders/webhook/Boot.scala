package kz.coders.webhook

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import kz.coders.webhook.actors.exchange.ExchangeWorkerActor
import kz.coders.webhook.actors.github.GithubWorkerActor
import kz.coders.webhook.actors.profitkz.{ArticlesWorkerActor, NewsWorkerActor}
import kz.coders.webhook.routes.Routes

object Boot extends App {
  implicit val system: ActorSystem = ActorSystem("chat-bot-webhook")
  implicit val materializer = Materializer(system)
  implicit val ex = system.dispatcher

  val config = ConfigFactory.load()

  val githubWorkerActor = system.actorOf(GithubWorkerActor.props(config))
  val exchangeWorkerActor = system.actorOf(ExchangeWorkerActor.props(config))
  val newsWorkerActor = system.actorOf(NewsWorkerActor.props(config))
  val articlesWorkerActor = system.actorOf(ArticlesWorkerActor.props(config))

  val routes = new Routes(
    system.log,
    githubWorkerActor,
    exchangeWorkerActor,
    newsWorkerActor,
    articlesWorkerActor
  )

  val host = config.getString("application.host")
  val port = config.getInt("application.port")

  Http().bindAndHandle(routes.handlers, host, port)

  system.log.info(s"running on $host:$port")
}
