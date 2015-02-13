package controllers

import models.User
import org.specs2.specification.Scope
import integration.WithTestDatabase
import org.specs2.execute.Results
import play.api.mvc.Controller
import play.api.test.{Helpers, FakeRequest, PlaySpecification}
import org.scalatest.mock.MockitoSugar
import session.SessionSpec
import session.SessionSpec.SessionTestComponentImpl

object UsersSpec extends PlaySpecification with Results with MockitoSugar with WithTestDatabase {

  def beforeAll() = {
    new UsersTestController().sessionManager.redis.del(SessionSpec.testUUID)
  }

  class UsersTestController() extends Controller with Users with SessionTestComponentImpl

  "UsersTestController#login" should {
    "should return invalid_params in case of wrong parameters name" in {
      val controller = new UsersTestController()
      val result = controller.login().apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText must contain("invalid_params")
    }
    "should return invalid_params and cause in case of empty values" in {

      val controller = new UsersTestController()
      val request = FakeRequest(Helpers.POST, controllers.routes.Users.login().url)
          .withFormUrlEncodedBody(
            "nickname" -> "",
            "password" -> "")

      val result = call(controller.login, request)
      val bodyText: String = contentAsString(result)
      bodyText must contain("invalid_params")
    }
    "should return user_not_found in case of user not found" in new CreateUser {

      val controller = new UsersTestController()
      val request = FakeRequest(Helpers.POST, controllers.routes.Users.login().url)
          .withFormUrlEncodedBody(
            "nickname" -> "xxx",
            "password" -> "xxx")

      val result = call(controller.login, request)
      val bodyText: String = contentAsString(result)
      bodyText must contain("user_not_found")
    }
    "should return success and have uuid in case of user found" in new CreateUser {

      val controller = new UsersTestController()
      val request = FakeRequest(Helpers.POST, controllers.routes.Users.login().url)
          .withFormUrlEncodedBody(
            "nickname" -> "test",
            "password" -> "4297f44b13955235245b2497399d7a93")

      val result = call(controller.login, request)
      val bodyText: String = contentAsString(result)

      bodyText must contain("success")
      bodyText must contain(SessionSpec.testUUID)
    }
  }

  class CreateUser extends Scope {
    val userID = User.save(User(Some(1), "test", "4297f44b13955235245b2497399d7a93"))
  }
}
