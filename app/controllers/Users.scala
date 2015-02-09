package controllers

import java.security.MessageDigest
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.redis.RedisClient
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc.{Result, Action, Controller}
import akka.util.Timeout
import play.api.db.DB

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await}

trait Users {
  this: Controller =>

  implicit val system = ActorSystem("redis-client")
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  val redis = RedisClient("localhost", 6379)

  def login = Action { implicit request =>

    val params:Map[String, Seq[String]] = request.body.asFormUrlEncoded.filter(
      k => k.getOrElse("nickname", List()).size > 0 ||
          k.getOrElse("password", List()).size > 0 ).getOrElse(Map[String, Seq[String]]())

    if (params.size == 2) {

      val nickname:String = params.get("nickname").get(0)
      val password:String = params.get("password").get(0)

      if (!nickname.isEmpty && !password.isEmpty) {

        val hash = MessageDigest
            .getInstance("MD5")
            .digest(password.getBytes)
            .map("%02x".format(_)).mkString

        val conn = DB.getConnection()

        try {

          val stmt = conn.createStatement
          val rs = stmt.executeQuery("SELECT * FROM user WHERE nickname = '"+
              nickname+"' AND password = '"+
              hash+"'")

          if (rs.next()) {

            val uuid = java.util.UUID.randomUUID.toString
            redis.set(uuid, "" + rs.getInt("id") + "|" + System.currentTimeMillis())

            Ok(Json.obj("status" -> "success", "uuid" -> uuid))

          } else {
            BadRequest(Json.obj("status" -> "error", "cause" -> "not_found"))
          }

        } finally {
          conn.close()
        }

      } else {
        BadRequest(Json.obj("status" -> "error", "cause" -> "invalid_params"))
      }
    } else {
      BadRequest(Json.obj("status" -> "error", "cause" -> "invalid_params"))
    }
  }

  def status = Action { implicit request =>

    val params:Map[String, Seq[String]] = request.body.asFormUrlEncoded.filter(
      k => k.getOrElse("auth", List()).size > 0 ).getOrElse(Map[String, Seq[String]]())

    if (params.size == 1) {

      val uuid:String = params.get("auth").get(0)
      if (!uuid.isEmpty) {

        val future: Future[Result] = {

          def f(x:Option[String]) = if (x != None) {
            Ok(Json.obj("status" -> "success", "uuid" -> x.get))
          } else {
            BadRequest(Json.obj("status" -> "error", "cause" -> "invalid_params"))
          }

          redis.get(uuid).map( k => f(k))
        }

        Await.result(future, Duration(5, TimeUnit.SECONDS))

      } else {
        BadRequest(Json.obj("status" -> "error", "cause" -> "invalid_params"))
      }
    } else {
      BadRequest(Json.obj("status" -> "error", "cause" -> "invalid_params"))
    }
  }
}

object Users extends Controller with Users
