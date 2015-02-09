package controllers

import java.util.concurrent.TimeUnit

import actors.UserActor
import akka.actor.ActorSystem
import com.redis.RedisClient
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller, WebSocket}
import akka.util.Timeout

import scala.concurrent.Future

object Application extends Controller {

  implicit val system = ActorSystem("redis-client")
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  val redis = RedisClient("localhost", 6379)

  def index = Action { implicit request =>
    Ok(views.html.login())
  }

  def ws = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>

    var uuid = ""
    request.cookies.get("auth") match {
      case Some(auth) => uuid = auth.value
      case None => uuid = ""
    }

    if (!uuid.isEmpty) {

      def f(x:Option[String]) = if (x != None) {
        Right(UserActor.props(x.get) _)
      } else {
        Left(Forbidden)
      }

      redis.get(uuid).map( k => f(k))

    } else {
      Future.successful(Left(Forbidden))
    }
  }

}