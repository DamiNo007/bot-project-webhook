package kz.coders.webhook.actors.exchange

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.Config
import kz.coders.webhook.actors.{Convert, GetCurrencies, GetRates, ReceivedFailureResponse}
import kz.coders.webhook.actors.exchange.ExchangeRequesterActor._
import org.json4s.{DefaultFormats, Formats}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ExchangeWorkerActor {
  def props(config: Config)(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new ExchangeWorkerActor(config))
}

class ExchangeWorkerActor(config: Config)(implicit val system: ActorSystem,
                                          materializer: Materializer)
  extends Actor with ActorLogging {

  implicit val ex: ExecutionContext = context.dispatcher
  implicit val formats: Formats = DefaultFormats
  implicit val timeout: Timeout = 100.seconds

  val requestActor: ActorRef =
    context.actorOf(Props(new ExchangeRequesterActor(config)))

  override def receive: Receive = {
    case GetCurrencies(msg) =>
      log.info(s"got msg $msg")
      val sender = context.sender()
      (requestActor ? GetAllCurrencies(msg)).onComplete {
        case Success(value) => sender ! value
        case Failure(e) => sender ! ReceivedFailureResponse(e.getMessage)
      }
    case GetRates(currency) =>
      val sender = context.sender()
      (requestActor ? GetRatesAll(currency)).onComplete {
        case Success(value) => sender ! value
        case Failure(e) => sender ! ReceivedFailureResponse(e.getMessage)
      }
    case Convert(from, to, amount) =>
      val sender = context.sender()
      (requestActor ? GetConvertResult(from, to, amount)).onComplete {
        case Success(value) => sender ! value
        case Failure(e) => sender ! ReceivedFailureResponse(e.getMessage)
      }
  }
}