package controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import models.User
import session.{SessionRepository, SessionRepositoryComponentImpl}

trait Users extends SessionRepository {
  this: Controller =>
    def login = Action { implicit request =>

      val params = request.body.asFormUrlEncoded.filter(k =>
        k.get("nickname").isDefined || k.get("password").isDefined
      )

      if (params.getOrElse(Map()).size >= 2) {

        val nickname: String = params.get.get("nickname").get(0)
        val password: String = params.get.get("password").get(0)

        if (!nickname.isEmpty && !password.isEmpty) {

          val users = User.findBy(nickname, password)
          users.lift(0) match {
            case Some(user) => Ok(Json.obj(
              "status" -> "success",
              "uuid" -> sessionManager.authorize(user)
            ))
            case None => BadRequest(Json.obj("status" -> "error", "cause" -> "user_not_found"))
          }

        } else {
          BadRequest(Json.obj("status" -> "error", "cause" -> "invalid_params"))
        }
      } else {
        BadRequest(Json.obj("status" -> "error", "cause" -> "invalid_params"))
      }
    }

    def status = Action { implicit request =>

      val params = request.body.asFormUrlEncoded.filter(k =>
        k.get("auth").isDefined
      )

      if (params.getOrElse(Map()).size == 1) {

        val uuid: String = params.get.get("auth").get(0)
        if (!uuid.isEmpty) {

          sessionManager.authorized(uuid) match {
            case Some(s) => Ok(Json.obj("status" -> "success", "uuid" -> s))
            case None => BadRequest(Json.obj("status" -> "error", "cause" -> "invalid_params"))
          }

        } else {
          BadRequest(Json.obj("status" -> "error", "cause" -> "invalid_params"))
        }
      } else {
        BadRequest(Json.obj("status" -> "error", "cause" -> "invalid_params"))
      }
    }
}

object Users extends Controller
  with Users
  with SessionRepositoryComponentImpl
