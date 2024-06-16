package service

import jakarta.inject.{Inject, Singleton}
import model.Ticket
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ElasticSearchService @Inject()(implicit ec: ExecutionContext, ws: WSClient) {

  private val baseUrl = "http://34.170.165.7:9200"
  private val indexName = "my_index"

  private val username = "elastic"
  private val password = "changeme"

  def getFromElastic(): Future[List[Ticket]] = {
    val url = s"$baseUrl/$indexName/_search"

    val jsonBody = Json.obj()

    val authHeader = basicAuthHeader(username, password)

    val request: WSRequest = ws.url(url)
      .addHttpHeaders("Content-Type" -> "application/json")
      .addHttpHeaders("Authorization" -> authHeader)

    request.post(jsonBody).flatMap { response: WSResponse =>
      response.status match {
        case 200 =>
          println("Aniket success logs : " + response.body)
          parseTickets(response.json)
        case _ =>
          println("Aniket failed logs : " + response.body)
          println(s"API call failed with status ${response.status}")
          Future.failed(new Exception(s"API call failed with status ${response.status}"))
      }
    }
  }

  private def parseTickets(json: JsValue): Future[List[Ticket]] = {
    val hits = (json \ "hits" \ "hits").asOpt[Seq[JsValue]].getOrElse(Seq.empty)
    val tickets = hits.map(hit => (hit \ "_source").as[Ticket])
    Future.successful(tickets.toList)
  }

  private def basicAuthHeader(username: String, password: String): String = {
    val auth = username + ":" + password
    "Basic " + java.util.Base64.getEncoder.encodeToString(auth.getBytes("utf-8"))
  }
}
