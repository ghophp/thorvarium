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

        val nickname: String = params.get("nickname").head
        val password: String = params.get("password").head

        if (!nickname.isEmpty && !password.isEmpty) {

          val users = User.findBy(nickname, password)
          users.headOption match {
            case Some(user) => Ok(Json.obj(
              "status" -> "success",
              "uuid" -> sessionManager.authorize(user),
              "user" -> user.toJson
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

    def logout = Action { implicit request =>

      val params = request.body.asFormUrlEncoded.filter(k =>
        k.get("auth").isDefined
      )

      if (params.getOrElse(Map()).size == 1) {

        val uuid: String = params.get("auth").head
        if (!uuid.isEmpty) {

          sessionManager.revoke(uuid)
          Ok(Json.obj("status" -> "success"))

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

        val uuid: String = params.get("auth").head
        if (!uuid.isEmpty) {

          sessionManager.authorized(uuid) match {
            case Some(s) => Ok(Json.obj("status" -> "success", "value" -> s.toJson))
            case None => BadRequest(Json.obj("status" -> "error", "cause" -> "unauthorized"))
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
