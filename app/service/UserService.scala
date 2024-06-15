package service

import jakarta.inject.{Inject, Singleton}
import model.User
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserService @Inject()(implicit ec: ExecutionContext, ws: WSClient){
  def getUserByToken(token: String): Future[User] = {
    println("calling api with token  :  + " + token )
    ws.url("http://userservice:9000/usersByToken")
      .addHttpHeaders("Content-Type" -> "application/json")
      .addHttpHeaders("token" -> token)
      .get()
      .flatMap { response =>
        response.status match {
          case 200 =>
           println("successful")
            Future.successful(response.json.as[User])
          case ex =>
            println(response)
            println(response.body)
            println("API called failed")
            Future.failed(new Exception("API called failed " + ex))
        }
      }
  }
  def getUserById(id: Int): Future[User] = {
    ws.url("http://userservice:9000/users/" + id)
      .addHttpHeaders("Content-Type" -> "application/json")
      .get()
      .flatMap { response =>
        response.status match {
          case 200 =>

            println("successful")
            Future.successful(response.json.as[User])
          case _ =>
            println(response)
            println(response.status)
            println("API called failed")
            Future.failed(new Exception("API called failed"))
        }
      }
  }
}
