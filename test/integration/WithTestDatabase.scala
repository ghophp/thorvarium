package integration

import org.specs2.specification._
import org.specs2.execute.AsResult
import play.api.test.Helpers._
import play.api.test.FakeApplication

trait WithTestDatabase extends AroundExample {

  val testDb = Map("db.default.url" -> "jdbc:sqlite:db/test.sqlite")

  def around[T : AsResult](t: => T) = {
    val app = FakeApplication(additionalConfiguration = testDb)
    running(app) {
      AsResult(t)
    }
  }
}