package service

import daos.TicketDAO
import model.Ticket

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TicketService @Inject()(ticketDAO: TicketDAO, userService: UserService, kafkaProducerService: KafkaProducerService)(implicit ec: ExecutionContext) {


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
      .flatMap{ ticketRes =>
        kafkaProducerService.sendMessage("checkTopic", ticketRes.toString)
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
                Future.failed(new Exception("Failed to retrieve tickers", e))
            }
          } else {
            ticketDAO.getById(id, None).recoverWith {
              case e: Exception =>
                Future.failed(new Exception("Failed to retrieve tickers", e))
            }
          }
      }
  }

  def updateTicketStatus(id: Int, status: String): Future[Option[Ticket]] = {
    ticketDAO.updateStatus(id, status)
      .flatMap(count => getTicketAfterUpdate(id, count))
  }

  def deleteTicket(id: Int): Future[Int] = ticketDAO.delete(id)

  def assignTicket(id: Int, assignTo: Option[Int], token: String): Future[Option[Ticket]] = {
    userService.getUserByToken(token)
      .flatMap {
        user =>
          if (user.role.toLowerCase != "admin") {
            Future.failed(new Exception("Only admin can assign tickets"))
          } else {
            ticketDAO.assignTicket(id, assignTo)
              .flatMap(count => getTicketAfterUpdate(id, count))
          }
      }
  }

  private def getTicketAfterUpdate(id: Int, updatedCount: Int): Future[Option[Ticket]] = {
    if (updatedCount == 1) {
      ticketDAO.getById(id, None)
    } else {
      Future.failed(new Exception("Not updated"))
    }
  }
}
