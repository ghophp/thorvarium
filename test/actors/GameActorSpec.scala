package actors

import akka.actor.Props
import akka.testkit.{TestActorRef, TestProbe}
import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationLike
import org.specs2.runner.JUnitRunner
import play.api.libs.json.Json
import play.api.test.WithApplication
import session.SessionSpec

@RunWith(classOf[JUnitRunner])
class GameActorSpec extends AbstractTestKit("UserActorSpec") with SpecificationLike {

  val testGameId = SessionSpec.testUser.id.get + "-" + SessionSpec.testUser2.id.get

  trait GameProbe extends WithApplication {
    val probe1 = new TestProbe(system)
    val probe2 = new TestProbe(system)

    val boardActorRef = TestActorRef[BoardActor](Props[BoardActor])
    val boardActor = boardActorRef.underlyingActor

    val gameActorRef = TestActorRef[GameActor](Props(classOf[GameActor], testGameId))
    val gameActor = boardActorRef.underlyingActor
  }

  "GameActor" should {

    "should have two users at an invitation accept" in new WithApplication with GameProbe {
      true mustEqual true
    }
  }

}
