package controllers

import scala.concurrent.Future

import actors.UserActor
import play.api.Logger
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.WebSocket

object Application extends Controller {
  val UID = "uid"
  var counter = 0;

  def index = Action { implicit request =>
    {
      val uid = request.session.get(UID).getOrElse {
        counter += 1
        counter.toString
      }
      Ok(views.html.index(uid)).withSession {
        Logger.debug("creation uid " + uid)
        request.session + (UID -> uid)
      }
    }
  }

  def ws = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
    Future.successful(request.session.get(UID) match {
      case None => Left(Forbidden)
      case Some(uid) => Right(UserActor.props(uid))
    })
  }

}