package actors

import integration.WithTestDatabase
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import play.api.test.WithApplication
import akka.actor.Props
import akka.testkit.TestActorRef
import akka.testkit.TestProbe

import play.api.libs.json.Json
import session.SessionSpec

class UserActorSpec extends AbstractTestKit("UserActorSpec") with SpecificationLike with WithTestDatabase {

  class BoardProbe extends Scope {

    val boardProbe = new TestProbe(system)
    val socketActorProbe = new TestProbe(system)

    val userActorRef = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser, socketActorProbe.ref))
    val userActor = userActorRef.underlyingActor
    userActor.board = boardProbe.ref

    val userActorRef2 = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser2, socketActorProbe.ref))
    val userActor2 = userActorRef2.underlyingActor
    userActor2.board = boardProbe.ref
  }

  class GameProbe extends BoardProbe {
    val gameProbe = new TestProbe(system)
  }

  "UserActor" should {

    "relay messages to the board" in new BoardProbe {
      val text = "test message"
      val testMsg = Json.obj("type" -> "message", "content" -> text)
      userActor.receive(testMsg)
      boardProbe.expectMsg(Message(SessionSpec.testUser, text))
    }

    "relay invitation to the board" in new BoardProbe {
      val testInvite = Json.obj("type" -> "invitation", "to" -> SessionSpec.testUser2.id)
      userActor.receive(testInvite)
      boardProbe.expectMsg(Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))
    }

    "relay accept invitation to the board" in new BoardProbe {
      val testAccept = Json.obj("type" -> "accept", "from" -> SessionSpec.testUser2.id.get)
      userActor.receive(testAccept)
      boardProbe.expectMsg(Accept(SessionSpec.testUser, SessionSpec.testUser2.id.get))
    }

    "toPlayerSet must translate json to objects" in new GameProbe {
      val playerSet = userActor.toPersons(SessionSpec.testPlayerSet)
      assert(
        playerSet.contains("person1") &&
          playerSet.contains("person2") &&
          playerSet.contains("person3"))
    }

    "relay player set to the game" in new GameProbe {

      val testPlayerSet = SessionSpec.testPlayerSet ++ Json.obj("type" -> "options")
      userActor.game = gameProbe.ref
      userActor.receive(testPlayerSet)

      val playerSet = userActor.toPersons(testPlayerSet)
      gameProbe.expectMsg(PlayerSet(SessionSpec.testUser.id.get, playerSet))
    }
  }

}
