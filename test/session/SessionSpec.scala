package session

import integration.WithTestDatabase
import models.User
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.PlaySpecification

class SessionSpec extends PlaySpecification with WithTestDatabase with MockitoSugar {

  def beforeAll() = {
    new SessionTest().sessionManager.redis.del(SessionSpec.testUUID)
  }

  trait SessionTestComponentImpl extends SessionRepositoryComponentImpl {

    override def sessionManager: Session = new SessionTestRepositoryImpl()

    class SessionTestRepositoryImpl extends SessionRepositoryImpl {
      override def uuid : String = {
        SessionSpec.testUUID
      }
    }
  }

  class SessionTest() extends SessionTestComponentImpl

  "SessionTest#redis" should {
    "should have a redis client" in {
      val sessionTest = new SessionTest()
      sessionTest.sessionManager.redis mustNotEqual null
    }
    "should return uuid on authorize" in {
      val sessionTest = new SessionTest()
      sessionTest.sessionManager.authorize(SessionSpec.testUser) mustEqual SessionSpec.testUUID
    }
    "should be authorized after set on authorize" in {
      val sessionTest = new SessionTest()
      val uuid = sessionTest.sessionManager.authorize(SessionSpec.testUser)
      sessionTest.sessionManager.authorized(uuid) must beSome
    }
  }
}

object SessionSpec extends SessionSpec {
  val testUUID : String = "046b6c7f-0b8a-43b9-b35d-6489e6daee91"
  val testUser : User = User(Some(1), "test", "test")
  val testUser2 : User = User(Some(2), "test2", "test2")
  val testUser3 : User = User(Some(3), "test3", "test3")
  val testPlayerSet = Json.obj("persons" -> Json.obj(
    "person1" -> Json.obj("id" -> 1, "weapon1" -> 1, "weapon2" -> 2),
    "person2" -> Json.obj("id" -> 2, "weapon1" -> 1, "weapon2" -> 2),
    "person3" -> Json.obj("id" -> 3, "weapon1" -> 1, "weapon2" -> 2)))
}
