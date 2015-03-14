package controllers

import actors.UserActor
import play.api.Play
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller, WebSocket}
import session.SessionRepositoryComponentImpl
import play.api.Play.current

import scala.concurrent.Future

object Application extends Controller with SessionRepositoryComponentImpl {

  def index = Action { implicit request =>
    Ok("We are live!")
  }

  def preFlight(all: String) = Action {

    val static = Play.current.configuration.getString("static.url").get

    Ok("").withHeaders(
      "Access-Control-Allow-Origin" -> static,
      "Allow" -> static,
      "Access-Control-Allow-Methods" -> "POST, GET, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referrer, User-Agent")
  }

  def ws = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>

    request.cookies.get("auth") match {
      case Some(auth) => Future.successful(sessionManager.authorized(auth.value) match {
        case Some(user) => Right(UserActor.props(user) _)
        case None => Left(Forbidden)
      })
      case None => Future.successful(Left(Forbidden))
    }
  }

}