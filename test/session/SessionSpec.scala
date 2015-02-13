package session

import integration.WithTestDatabase
import models.User
import org.scalatest.mock.MockitoSugar
import play.api.test.PlaySpecification

object SessionSpec extends PlaySpecification with WithTestDatabase with MockitoSugar {

  val testUUID : String = "046b6c7f-0b8a-43b9-b35d-6489e6daee91"
  val testUser : User = User(Some(1), "test", "test")

  def beforeAll() = {
    new SessionTest().sessionManager.redis.del(testUUID)
  }

  trait SessionTestComponentImpl extends SessionRepositoryComponentImpl {

    override def sessionManager: Session = new SessionTestRepositoryImpl()

    class SessionTestRepositoryImpl extends SessionRepositoryImpl {
      override def uuid : String = {
        testUUID
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
      sessionTest.sessionManager.authorize(testUser) mustEqual testUUID
    }
    "should be authorized after set on authorize" in {

      val sessionTest = new SessionTest()
      val uuid = sessionTest.sessionManager.authorize(testUser)

      sessionTest.sessionManager.authorized(uuid) mustEqual Some(testUUID)
    }
  }
}
