package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import service.TicketService
import model.Ticket

@Singleton
class TicketController @Inject()(cc: ControllerComponents, ticketService: TicketService)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val validDepartments = Set("Delivery", "Restaurant", "Payment", "General")
  private val validStatuses = Set("OPEN", "ASSIGNED", "RESOLVED")

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Ticket].map { ticket =>
      if (validDepartments.contains(ticket.department) && validStatuses.contains(ticket.status)) {
        ticketService.createTicket(ticket).map { id =>
          Created(Json.obj("id" -> id))
        }
      } else {
        Future.successful(BadRequest("Invalid Department or Status"))
      }
    }.getOrElse(Future.successful(BadRequest("Invalid format")))
  }

  def updateStatus(id: Int): Action[JsValue] = Action.async(parse.json) { request =>
    (request.body \ "status").asOpt[String].map { status =>
      if (validStatuses.contains(status)) {
        ticketService.updateTicketStatus(id, status).map {
          case 0 => NotFound
          case _ => Ok
        }
      } else {
        Future.successful(BadRequest("Invalid Status"))
      }
    }.getOrElse(Future.successful(BadRequest("Invalid format")))
  }

  def get(id: Int): Action[AnyContent] = Action.async {
    ticketService.getTicketById(id).map {
      case Some(ticket) => Ok(Json.toJson(ticket))
      case None => NotFound
    }
  }

  def delete(id: Int): Action[AnyContent] = Action.async {
    ticketService.deleteTicket(id).map {
      case 0 => NotFound
      case _ => Ok(Json.toJson("Deleted successfully"))
    }
  }

  def getAll: Action[AnyContent] = Action.async {
    ticketService.getAllTickets.map { tickets =>
      Ok(Json.toJson(tickets))
    }
  }
}
