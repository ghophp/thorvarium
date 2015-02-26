package actors

import play.api.libs.json.Json
import session.SessionSpec

import org.specs2.mutable.SpecificationLike
import org.specs2.time.NoTimeConversions
import akka.actor.Props
import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import play.api.test.WithApplication
import akka.actor.PoisonPill

class BoardActorSpec extends AbstractTestKit("BoardActorSpec") with SpecificationLike with NoTimeConversions {

  trait TwoActorProbe extends WithApplication {
    val probe1 = new TestProbe(system)
    val probe2 = new TestProbe(system)

    val boardActorRef = TestActorRef[BoardActor](Props[BoardActor])
    val boardActor = boardActorRef.underlyingActor
  }

  trait ThreeActorProbe extends TwoActorProbe {
    val probe3 = new TestProbe(system)
  }

  "BoardActor" should {

    "accept subscriptions" in new WithApplication with TwoActorProbe {

      assert(boardActor.users.size == 0)

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe2.send(boardActorRef, Subscribe(SessionSpec.testUser2))

      awaitCond(boardActor.users.size == 2)

      assert(boardActor.users.contains(probe1.ref))
      assert(boardActor.users.contains(probe2.ref))
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
      probe1.send(boardActorRef, Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))
      probe1.send(boardActorRef, Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))

      awaitCond(boardActor.invitations.size == 1)
    }

    "should not allow accept non existing invitations" in new WithApplication with ThreeActorProbe {

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe2.send(boardActorRef, Subscribe(SessionSpec.testUser2))
      probe3.send(boardActorRef, Subscribe(SessionSpec.testUser3))
      probe1.send(boardActorRef, Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))
      probe3.send(boardActorRef, Accept(SessionSpec.testUser3, SessionSpec.testUser2.id.get))

      awaitCond(boardActor.invitations.size == 1)
      awaitCond(boardActor.users.size == 3)
    }

    "on accept invitation create a game removing users from board" in new WithApplication with ThreeActorProbe {

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe2.send(boardActorRef, Subscribe(SessionSpec.testUser2))
      probe1.send(boardActorRef, Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))
      probe2.send(boardActorRef, Accept(SessionSpec.testUser2, SessionSpec.testUser.id.get))

      awaitCond(boardActor.invitations.size == 0)
      awaitCond(boardActor.games.size == 1)
      awaitCond(boardActor.users.size == 0)
    }

    "on accept invitation users receive start game message" in new WithApplication with ThreeActorProbe {

      probe1.send(boardActorRef, Subscribe(SessionSpec.testUser))
      probe2.send(boardActorRef, Subscribe(SessionSpec.testUser2))
      probe1.send(boardActorRef, Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))
      probe2.send(boardActorRef, Accept(SessionSpec.testUser2, SessionSpec.testUser.id.get))

      probe2.expectMsgClass(classOf[BoardMembers])
      probe2.expectMsgClass(classOf[Invitation])
      probe2.expectMsgClass(classOf[BoardMembers])
      probe2.expectMsgClass(classOf[StartGame])
    }
  }

}