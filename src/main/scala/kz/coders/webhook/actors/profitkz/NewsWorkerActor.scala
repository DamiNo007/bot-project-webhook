package kz.coders.webhook.actors.profitkz

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.Config
import kz.coders.webhook.actors.{GetNews, ReceivedFailureResponse}
import kz.coders.webhook.actors.profitkz.NewsRequesterActor._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object NewsWorkerActor {
  def props(config: Config)(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new NewsWorkerActor(config))
}

class NewsWorkerActor(config: Config)(implicit val system: ActorSystem,
                                      materializer: Materializer)
  extends Actor with ActorLogging {

  implicit val timeout: Timeout = 100.seconds
  implicit val ex: ExecutionContext = context.dispatcher
  val requestActor = context.actorOf(Props(new NewsRequesterActor(config)))

  override def receive: Receive = {
    case GetNews(msg) =>
      val sender = context.sender()
      (requestActor ? GetNewsAll(msg)).onComplete {
        case Success(value) =>
          sender ! value
        case Failure(e) =>
          sender ! ReceivedFailureResponse(e.getMessage)
      }
  }
}