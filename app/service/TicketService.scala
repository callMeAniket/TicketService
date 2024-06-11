package service

import javax.inject.{Inject, Singleton}
import model.Ticket
import daos.TicketDAO
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TicketService @Inject()(ticketDAO: TicketDAO)(implicit ec: ExecutionContext) {

  def getAllTickets: Future[Seq[Ticket]] = ticketDAO.all().recoverWith {
    case e: Exception =>
      Future.failed(new Exception("Failed to retrieve tickers", e))
  }

  def createTicket(ticket: Ticket): Future[Int] = ticketDAO.insert(ticket)

  def getTicketById(id: Int): Future[Option[Ticket]] = ticketDAO.getById(id)

  def updateTicketStatus(id: Int, status: String): Future[Int] = ticketDAO.updateStatus(id, status)

  def deleteTicket(id: Int): Future[Int] = ticketDAO.delete(id)
}
