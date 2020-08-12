package kz.coders.webhook.actors.profitkz

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.Config
import kz.coders.webhook.actors.{GetArticles, ReceivedFailureResponse}
import kz.coders.webhook.actors.profitkz.ArticlesRequesterActor._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ArticlesWorkerActor {
  def props(config: Config)(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new ArticlesWorkerActor(config))
}

class ArticlesWorkerActor(config: Config)(implicit val system: ActorSystem,
                                          materializer: Materializer)
  extends Actor with ActorLogging {

  implicit val timeout: Timeout = 100.seconds
  implicit val ex: ExecutionContext = context.dispatcher
  val requestActor = context.actorOf(Props(new ArticlesRequesterActor(config)))

  override def receive: Receive = {
    case GetArticles(msg) =>
      val sender = context.sender()
      (requestActor ? GetArticlesAll(msg)).onComplete {
        case Success(value) =>
          sender ! value
        case Failure(e) =>
          sender ! ReceivedFailureResponse(e.getMessage)
      }
  }
}