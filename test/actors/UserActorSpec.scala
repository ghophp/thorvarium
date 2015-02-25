package actors

import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationLike
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication
import akka.actor.Props
import akka.testkit.TestActorRef
import akka.testkit.TestProbe

import play.api.libs.json.Json
import session.SessionSpec

@RunWith(classOf[JUnitRunner])
class UserActorSpec extends AbstractTestKit("UserActorSpec") with SpecificationLike {

  class BoardProbe {

    val boardProbe = new TestProbe(system)
    val socketActorProbe = new TestProbe(system)

    val userActorRef = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser, socketActorProbe.ref))
    val userActor = userActorRef.underlyingActor
    userActor.board = boardProbe.ref

    val userActorRef2 = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser2, socketActorProbe.ref))
    val userActor2 = userActorRef2.underlyingActor
    userActor2.board = boardProbe.ref
  }

  "UserActor" should {

    "relay messages to the board" in new WithApplication {
      val probe = new BoardProbe()
      val text = "test message"
      val testMsg = Json.obj("type" -> "message", "content" -> text)
      probe.userActor.receive(testMsg)
      probe.boardProbe.expectMsg(Message(SessionSpec.testUser, text))
    }

    "relay invitation to the board" in new WithApplication {
      val probe = new BoardProbe()
      val testInvite = Json.obj("type" -> "invitation", "to" -> SessionSpec.testUser2.id)
      probe.userActor.receive(testInvite)
      probe.boardProbe.expectMsg(Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))
    }

    "relay accept invitation to the board" in new WithApplication {
      val probe = new BoardProbe()
      val testAccept = Json.obj("type" -> "accept", "from" -> SessionSpec.testUser2.id.get)
      probe.userActor.receive(testAccept)
      probe.boardProbe.expectMsg(Accept(SessionSpec.testUser, SessionSpec.testUser2.id.get))
    }
  }

}
