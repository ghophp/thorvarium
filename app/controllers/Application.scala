package controllers

import actors.UserActor
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller, WebSocket}
import session.SessionRepositoryComponentImpl
import play.api.Play.current

import scala.concurrent.Future

object Application extends Controller with SessionRepositoryComponentImpl {

  def index = Action { implicit request =>
    Ok("We are live!")
  }

  def ws = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>

    request.cookies.get("auth") match {
      case Some(auth) => Future.successful(sessionManager.redis.get(auth.value) match {
        case Some(x) => Right(UserActor.props(x) _)
        case None => Left(Forbidden)
      })
      case None => Future.successful(Left(Forbidden))
    }
  }

}