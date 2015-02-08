package controllers

import actors.UserActor
import play.api.Logger
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.WebSocket

object Application extends Controller {

  def index = Action {
    Ok("I'm alive!\n")
  }

  def ws = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    UserActor.props(out)
  }

}