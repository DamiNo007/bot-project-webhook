package kz.coders.webhook.actors.github

import akka.actor.{Actor, ActorSystem}
import akka.stream.Materializer
import com.typesafe.config.Config
import kz.coders.webhook.actors.{ReceivedFailureResponse, ReceivedResponse}
import kz.coders.webhook.actors.github.GithubRequesterActor._
import org.json4s.jackson.JsonMethods.parse
import org.json4s.{DefaultFormats, Formats, MappingException}
import kz.coders.webhook.utils.RestClientImpl._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object GithubRequesterActor {

  case class GetUserAccount(login: String)

  case class GetUserRepositories(login: String)

  case class GetUserAccountHttp(login: String)

  case class GetUserRepositoriesHttp(login: String)

  case class GithubUser(login: String,
                        name: String,
                        avatarUrl: Option[String],
                        publicRepos: Option[String])

  case class GithubRepository(name: String,
                              size: Int,
                              fork: Boolean,
                              pushedAt: String,
                              stargazersCount: Int)

}

class GithubRequesterActor(config: Config)(implicit val system: ActorSystem,
                                           materializer: Materializer)
  extends Actor {

  implicit val ex: ExecutionContext = context.dispatcher
  implicit val formats: Formats = DefaultFormats
  val baseUrl = config.getString("github.base-url")

  //  https://api.github.com/users/{$USER}
  def getGithubUser(username: String): Future[GithubUser] = {
    get(s"${baseUrl}/users/${username}", Nil)
      .map { body =>
        parse(body).camelizeKeys.extract[GithubUser]
      }
  }

  def getUserRepositories(username: String): Future[List[GithubRepository]] = {
    get(s"${baseUrl}/users/${username}/repos", Nil)
      .map { body =>
        parse(body).camelizeKeys.extract[List[GithubRepository]]
      }
  }

  def mkListOfString(list: List[GithubRepository]): List[String] = {
    list.zipWithIndex.map {
      case (GithubRepository(name, size, fork, pushedAt, stargazersCount), id) =>
        s"${id + 1}. $name: size = $size, stargazers = $stargazersCount, push date = $pushedAt, fork = ${
          if (fork)
            "Forked"
          else "Not Forked"
        }"
    }
  }

  override def receive: Receive = {
    case GetUserAccount(login) =>
      val sender = context.sender()
      getGithubUser(login).onComplete {
        case Success(user) =>
          sender ! ReceivedResponse(
            s"""
               |Full name: ${user.name}
               |Login: ${user.login}
               |Avatar:  ${user.avatarUrl.getOrElse("None")}
               |Repositories:  ${
              user.publicRepos
                .getOrElse("None")
            } """.stripMargin)
        case Failure(e: MappingException) =>
          sender ! ReceivedFailureResponse("Account does not exist!")
        case Failure(e) =>
          sender ! ReceivedFailureResponse("Connection error occured!")
      }
    case GetUserRepositories(login) =>
      val sender = context.sender()
      getUserRepositories(login).onComplete {
        case Success(response) =>
          val list = mkListOfString(response)
          val result =
            if (list.isEmpty)
              "Sorry, this account does not have any repositories yet!"
            else list.mkString("\n")
          sender ! ReceivedResponse(result)
        case Failure(e: MappingException) =>
          sender ! ReceivedFailureResponse("Account does not exist!")
        case Failure(e) =>
          sender ! ReceivedFailureResponse("Connection error occured!")
      }
  }
}