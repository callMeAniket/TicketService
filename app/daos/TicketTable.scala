package daos

import model.Ticket
import slick.jdbc.MySQLProfile.api._

class TicketTable(tag: Tag) extends Table[Ticket](tag, "Ticket") {
  def id = column[Int]("TicketID", O.PrimaryKey, O.AutoInc)
  def title = column[String]("Title")
  def description = column[Option[String]]("Description")
  def department = column[String]("Department")
  def status = column[String]("Status")
  def assignedTo = column[Option[Int]]("AssignedTo")

  def * = (id.?, title, description, department, status, assignedTo) <> ((Ticket.apply _).tupled, Ticket.unapply)
}

