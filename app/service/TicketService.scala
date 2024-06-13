package service

import javax.inject.{Inject, Singleton}
import model.Ticket
import daos.TicketDAO
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TicketService @Inject()(ticketDAO: TicketDAO, userService: UserService)(implicit ec: ExecutionContext) {

  def getAllTickets(token: String): Future[Seq[Ticket]] = {
    userService.getUserByToken(token)
      .flatMap{
        user => if(user.role.toLowerCase != "admin") {
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

  def createTicket(ticket: Ticket): Future[Int] = ticketDAO.insert(ticket)

  def getTicketById(id: Int, token: String): Future[Option[Ticket]] = {
    userService.getUserByToken(token)
      .flatMap{
        user => if(user.role.toLowerCase != "admin") {
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

  def updateTicketStatus(id: Int, status: String): Future[Int] = ticketDAO.updateStatus(id, status)

  def deleteTicket(id: Int): Future[Int] = ticketDAO.delete(id)
}
