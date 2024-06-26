package daos

import model.Ticket
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TicketDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {

  val dbConfig = dbConfigProvider.get[MySQLProfile]
  import dbConfig._
  import profile.api._
  private val tickets = TableQuery[TicketTable]

  def all(id : Option[Int]): Future[Seq[Ticket]] =
    id match {
      case Some(userId) => db.run(tickets.filter(_.assignedTo === userId).result)
      case _ => db.run(tickets.result)
    }

  def insert(ticket: Ticket): Future[Int] = db.run(tickets returning tickets.map(_.id) += ticket)

  def getById(id: Int, userIdOpt: Option[Int]): Future[Option[Ticket]] =
    userIdOpt match {
      case Some(userId) => db.run(tickets.filter(ticket => ticket.id === id && ticket.assignedTo === userId).result.headOption)
      case _ => db.run(tickets.filter(_.id === id).result.headOption)
    }

  def updateStatus(id: Int, status: String): Future[Int] = db.run(tickets.filter(_.id === id).map(_.status).update(status))

  def delete(id: Int): Future[Int] = db.run(tickets.filter(_.id === id).delete)

  def assignTicket(id: Int, assignTo: Option[Int]): Future[Int] = db.run(tickets.filter(_.id ===id).map(_.assignedTo).update(assignTo))
}
