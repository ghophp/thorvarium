package controllers

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.redis.RedisClient
import play.api.Play.current
import play.api.libs.json.{JsObject, Json, JsValue}
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.WebSocket
import akka.util.Timeout
import play.api.db.DB

import scala.concurrent.Future

object Application extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.login())
  }

  def login = Action(parse.tolerantFormUrlEncoded) { implicit request =>

    val conn = DB.getConnection()
    try {
      val stmt = conn.createStatement
      val rs = stmt.executeQuery("SELECT * FROM user ")
      while (rs.next()) {
        println(rs.getString("nickname"))
      }
    } finally {
      conn.close()
    }

    implicit val system = ActorSystem("redis-client")
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)

    val user:String = request.body.get("user").toString
    val redis = RedisClient("localhost", 6379)

    def result:JsObject = redis.exists(user).value.getOrElse(false) match {
      case Some(true) =>

        redis.get(user).value.getOrElse("") match {
          case uuid:String => Json.obj("status" -> "success", "uuid" -> uuid)
          case _ => Json.obj("status" -> "error")
        }

      case _ =>
        Json.obj("status" -> "success", "uuid" -> "xxx")
    }

    Ok(result)
  }

  def ws = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
    Future.successful("" match {
      case _ => Left(Forbidden)
    })
  }

}