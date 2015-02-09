package controllers

import org.specs2.execute.Results
import play.api.mvc.Controller
import play.api.test.{FakeHeaders, Helpers, FakeRequest, PlaySpecification}

object UsersSpec extends PlaySpecification with Results {

  class UsersTestController() extends Controller with Users

  "UsersTestController#login" should {
    "should return 400 in case of wrong parameters name" in {
      val controller = new UsersTestController()
      val result = controller.login().apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText must be equalTo "{\"status\":\"error\",\"cause\":\"invalid_params\"}"
    }
    "should return 400 in case of empty values" in {

      val controller = new UsersTestController()
      val request:FakeRequest[String] = FakeRequest(Helpers.POST,
        controllers.routes.Users.login().url,
        FakeHeaders(),
        "nickname=&password=")

      val result = call(controller.login, request)
      val bodyText: String = contentAsString(result)
      bodyText must be equalTo "{\"status\":\"error\",\"cause\":\"invalid_params\"}"
    }
  }
}
