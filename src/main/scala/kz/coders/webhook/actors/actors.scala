package kz.coders.webhook

package object actors {

  trait Response

  trait Request

  case class GetUser(login: String) extends Request

  case class GetRepositories(login: String) extends Request

  case class GetNews(msg: String) extends Request

  case class GetNewsHttp(msg: String) extends Request

  case class GetArticles(msg: String) extends Request

  case class GetArticlesHttp(msg: String) extends Request

  case class GetCurrencies(msg: String) extends Request

  case class GetCurrenciesHttp(msg: String) extends Request

  case class GetRates(currency: String) extends Request

  case class GetRatesHttp(currency: String) extends Request

  case class Convert(from: String, to: String, amount: String) extends Request

  case class ReceivedResponse(response: String) extends Response

  case class ReceivedFailureResponse(error: String) extends Response

}
