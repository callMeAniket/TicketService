package service

import daos.TicketDAO
import model.Ticket
import play.api.libs.json.Json
import play.filters.csrf.CSRF.Token

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TicketService @Inject()(ticketDAO: TicketDAO, userService: UserService, kafkaProducerService: KafkaProducerService, elasticSearchService: ElasticSearchService)(implicit ec: ExecutionContext) {


  def getAllTickets(token: String): Future[Seq[Ticket]] = {
    userService.getUserByToken(token)
      .flatMap {
        user =>
          if (user.role.toLowerCase != "admin") {
            ticketDAO.all(user.id).recoverWith {
              case e: Exception =>
                Future.failed(new Exception("Failed to retrieve tickers", e))
            }
          } else {
            ticketDAO.all(None).recoverWith {
              case e: Exception =>
                Future.failed(new Exception("Failed to retrieve tickers", e))
            }
          }
      }
  }

  def createTicket(ticket: Ticket): Future[Option[Ticket]] = {
    ticketDAO.insert(ticket)
      .flatMap(id => ticketDAO.getById(id, None)
        .flatMap { ticketRes =>
          val ticketJson = Json.toJson(ticketRes).toString()
          kafkaProducerService.sendMessage(ticketJson)
          Future.successful(ticketRes)
        })
  }

  def getTicketById(id: Int, token: String): Future[Option[Ticket]] = {
    userService.getUserByToken(token)
      .flatMap {
        user =>
          if (user.role.toLowerCase != "admin") {
            ticketDAO.getById(id, user.id).recoverWith {
              case e: Exception =>
                Future.failed(new Exception("Failed to retrieve tickets", e))
            }
          } else {
            getTicket(id)
          }
      }
  }

  private def getTicket(id: Int): Future[Option[Ticket]] = {
    ticketDAO.getById(id, None).recoverWith {
      case e: Exception =>
        Future.failed(new Exception("Failed to retrieve tickets", e))
    }
  }


  def updateTicketStatus(id: Int, status: String, token: String): Future[Option[Ticket]] = {
    getTicket(id)
      .flatMap {
        case Some(t) => userService.getUserByToken(token)
          .flatMap {
            user =>
              if (user.role.toLowerCase != "admin") {
                if (t.assignedTo == user.id) {
                  ticketDAO.updateStatus(id, status)
                    .flatMap(count => getTicketAfterUpdate(id, count))
                } else {
                  Future.failed(new Exception("Only assigned can update tickets"))
                }
              } else {
                ticketDAO.updateStatus(id, status)
                  .flatMap(count => getTicketAfterUpdate(id, count))
              }
          }
        case (_) => Future.failed(new Exception("Failed to retrieve ticket"))
      }
  }

  def deleteTicket(id: Int, token: String): Future[Int] = {
    getTicket(id)
      .flatMap {
        case Some(t) => userService.getUserByToken(token)
          .flatMap {
            user =>
              if (user.role.toLowerCase != "admin") {
                Future.failed(new Exception("Only admin can delete tickets"))
              } else {
                ticketDAO.delete(id)
              }
          }
        case (_) => Future.failed(new Exception("Failed to retrieve ticket"))
      }
  }

  def assignTicket(id: Int, assignTo: Option[Int], token: String): Future[Option[Ticket]] = {
    getTicket(id)
      .flatMap {
        case Some(t) => userService.getUserByToken(token)
          .flatMap {
            user =>
              if (user.role.toLowerCase != "admin") {
                Future.failed(new Exception("Only admin can assign tickets"))
              } else {
                ticketDAO.assignTicket(id, assignTo)
                  .flatMap(count => getTicketAfterUpdate(id, count))
              }
          }
        case (_) => Future.failed(new Exception("Failed to retrieve ticket"))
      }
  }

  private def getTicketAfterUpdate(id: Int, updatedCount: Int): Future[Option[Ticket]] = {
    if (updatedCount == 1) {
      ticketDAO.getById(id, None)
    } else {
      Future.failed(new Exception("Not updated"))
    }
  }

  def getAllTicketsFromElastic(token: String): Future[List[Ticket]] = {
    userService.getUserByToken(token)
      .flatMap {
        user =>
          if (user.role.toLowerCase != "admin") {
            elasticSearchService.getFromElastic().map { tickets =>
              tickets.filter(ticket => ticket.assignedTo.get == user.id.get)
            }
          } else {
            elasticSearchService.getFromElastic()
          }
      }
  }
}
