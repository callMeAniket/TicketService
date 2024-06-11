package model

import play.api.libs.json.{Json, OFormat}

case class Ticket(
                   id: Option[Int],
                   title: String,
                   description: Option[String],
                   department: String,
                   status: String,
                   assignedTo: Option[Int]
                 )

object Ticket {
  implicit val ticketFormat: OFormat[Ticket] = Json.format[Ticket]
}
