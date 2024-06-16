package controllers

import model.Ticket
import play.api.libs.json._
import play.api.mvc._
import service.TicketService

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TicketController @Inject()(cc: ControllerComponents, ticketService: TicketService)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val validDepartments = Set("Delivery", "Restaurant", "Payment", "General")
  private val validStatuses = Set("OPEN", "ASSIGNED", "RESOLVED")

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Ticket].map { ticket =>
      if (validDepartments.contains(ticket.department) && validStatuses.contains(ticket.status)) {
        ticketService.createTicket(ticket).map { ticket =>
          Created(Json.toJson(ticket))
        }
      } else {
        Future.successful(BadRequest("Invalid Department or Status"))
      }
    }.getOrElse(Future.successful(BadRequest("Invalid format")))
  }

  def updateStatus(id: Int): Action[JsValue] = Action.async(parse.json) { request =>
    (request.body \ "status").asOpt[String].map { status =>
      if (validStatuses.contains(status.toUpperCase)) {
        ticketService.updateTicketStatus(id, status.toUpperCase(), request.headers.get("token").get).map {
          case None => NotFound
          case _ => Ok
        }
      } else {
        Future.successful(BadRequest("Invalid Status"))
      }
    }.getOrElse(Future.successful(BadRequest("Invalid format")))
  }

  def assignTicket(id: Int): Action[JsValue] = Action.async(parse.json) { request =>
    request.headers.get("token") match {
      case Some(token) =>
        (request.body \ "assignTo").asOpt[Int].map { assignTo =>
          if (assignTo != 0) {
            ticketService.assignTicket(id, Some(assignTo), token).map {
              case None => NotFound
              case _ => Ok
            }
          } else {
            Future.successful(BadRequest("Assigned To is required"))
          }
        }.getOrElse(Future.successful(BadRequest("Invalid format")))
      case _ => Future.successful(BadRequest("Admin Token Required"))
    }
  }

  def get(id: Int): Action[AnyContent] = Action.async { request =>
    ticketService.getTicketById(id, request.headers.get("token").get).map {
      case Some(ticket) => Ok(Json.toJson(ticket))
      case None => NotFound
    }
  }

  def delete(id: Int): Action[AnyContent] = Action.async {
    request => {
      ticketService.deleteTicket(id, request.headers.get("token")).map {
        case 0 => NotFound
        case _ => Ok(Json.toJson("Deleted successfully"))
      }
    }
  }

  def getAll: Action[AnyContent] = Action.async { request =>
    request.headers.get("token") match {
      case Some(token) =>
        ticketService.getAllTickets(token)
          .map { tickets =>
            Ok(Json.toJson(tickets))
          }
      case _ => Future.successful(BadRequest(""))
    }
  }
  def getAllFromElastic: Action[AnyContent] = Action.async { request =>
    request.headers.get("token") match {
      case Some(token) =>
        ticketService.getAllTicketsFromElastic(token)
          .map { tickets =>
            Ok(Json.toJson(tickets))
          }
      case _ => Future.successful(BadRequest(""))
    }
  }
}
