package actors

import play.api.libs.json.Json
import session.SessionSpec

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

  trait TwoActorProbe {

    val probe1 = new TestProbe(system)
    val probe2 = new TestProbe(system)

    val boardActorRef = TestActorRef[BoardActor](Props[BoardActor])
    val boardActor = boardActorRef.underlyingActor
  }

  "BoardActor" should {

    "accept subscriptions" in new WithApplication with TwoActorProbe {

      assert(boardActor.users.size == 0)

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe2.send(boardActorRef, Subscribe(SessionSpec.testUser2))

      awaitCond(boardActor.users.size == 2)

      assert(boardActor.users.contains((probe1.ref, SessionSpec.testUser)))
      assert(boardActor.users.contains((probe2.ref, SessionSpec.testUser2)))
    }

    "watch its users" in new WithApplication with TwoActorProbe {

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe2.send(boardActorRef, Subscribe(SessionSpec.testUser2))

      awaitCond(boardActor.users.size == 2)
      probe2.ref ! PoisonPill
      awaitCond(boardActor.users.size == 1)
    }

    "return members at subscriptions" in new WithApplication with TwoActorProbe {
      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe1.expectMsg(BoardMembers(Json.arr(SessionSpec.testUser.toJson)))
    }

    "watch its invitations" in new WithApplication with TwoActorProbe {

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe2.send(boardActorRef, Subscribe(SessionSpec.testUser2))
      probe1.send(boardActorRef, Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))

      awaitCond(boardActor.invitations.size == 1)

      probe2.ref ! PoisonPill

      awaitCond(boardActor.users.size == 1)
      awaitCond(boardActor.invitations.size == 0)
    }

    "should not allow duplicated invitations" in new WithApplication with TwoActorProbe {

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe2.send(boardActorRef, Subscribe(SessionSpec.testUser2))

      awaitCond(boardActor.users.size == 2)

      probe1.send(boardActorRef, Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))
      probe1.send(boardActorRef, Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))

      awaitCond(boardActor.invitations.size == 1)
    }

    "should not allow duplicated invitations" in new WithApplication with TwoActorProbe {

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe2.send(boardActorRef, Subscribe(SessionSpec.testUser2))
      probe1.send(boardActorRef, Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))
      probe1.send(boardActorRef, Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))

      awaitCond(boardActor.invitations.size == 1)
    }
  }

}