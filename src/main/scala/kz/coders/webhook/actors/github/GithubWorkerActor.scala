package kz.coders.webhook.actors.github

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.Config
import kz.coders.webhook.actors.{GetRepositories, GetUser, ReceivedFailureResponse}
import kz.coders.webhook.actors.github.GithubRequesterActor._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object GithubWorkerActor {
  def props(config: Config)(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new GithubWorkerActor(config))
}

class GithubWorkerActor(config: Config)(implicit val system: ActorSystem,
                                        materializer: Materializer)
  extends Actor with ActorLogging {

  implicit val timeout: Timeout = 100.seconds
  implicit val ex: ExecutionContext = context.dispatcher
  val requestActor = context.actorOf(Props(new GithubRequesterActor(config)))

  override def receive: Receive = {
    case GetUser(login) =>
      val sender = context.sender()
      (requestActor ? GetUserAccount(login)).onComplete {
        case Success(value) =>
          sender ! value
        case Failure(e) =>
          sender ! ReceivedFailureResponse(e.getMessage)
      }
    case GetRepositories(login) =>
      val sender = context.sender()
      (requestActor ? GetUserRepositories(login)).onComplete {
        case Success(value) => sender ! value
        case Failure(e) =>
          sender ! ReceivedFailureResponse(e.getMessage)
      }
  }
}
