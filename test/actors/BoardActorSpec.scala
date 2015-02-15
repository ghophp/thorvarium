package actors

import play.api.libs.json.Json
import session.SessionSpec

import scala.concurrent.duration.DurationInt
import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions
import akka.actor.Props
import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import play.api.test.WithApplication
import org.specs2.runner.JUnitRunner
import akka.actor.PoisonPill

@RunWith(classOf[JUnitRunner])
class BoardActorSpec extends AbstractTestKit("BoardActorSpec") with SpecificationLike with NoTimeConversions {

  "BoardActor" should {
    val probe1 = new TestProbe(system)
    val probe2 = new TestProbe(system)

    "accept subscriptions" in new WithApplication {
      val boardActorRef = TestActorRef[BoardActor](Props[BoardActor])
      val boardActor = boardActorRef.underlyingActor

      assert(boardActor.users.size == 0)

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe2.send(boardActorRef, Subscribe(SessionSpec.testUser2))

      awaitCond(boardActor.users.size == 2)

      assert(boardActor.users.contains((probe1.ref, SessionSpec.testUser)))
      assert(boardActor.users.contains((probe2.ref, SessionSpec.testUser2)))

    }

    "and watch its users" in new WithApplication {
      val boardActorRef = TestActorRef[BoardActor](Props[BoardActor])
      val boardActor = boardActorRef.underlyingActor

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe2.send(boardActorRef, Subscribe(SessionSpec.testUser2))

      awaitCond(boardActor.users.size == 2)
      
      probe2.ref ! PoisonPill
      
      awaitCond(boardActor.users.size == 1)
    }

    "return members at subscriptions" in new WithApplication() {

      val boardActorRef = TestActorRef[BoardActor](Props[BoardActor])
      val boardActor = boardActorRef.underlyingActor

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe1.expectMsg(BoardMembers(Json.arr(SessionSpec.testUser.toJson)))
    }
  }

}