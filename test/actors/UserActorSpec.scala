package actors

import game.models.GamingSet
import integration.WithTestDatabase
import models.Person
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
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

    "toOptions must translate json to player options to start the game" in new GameProbe {
      val playerSet = Person.toPersons(SessionSpec.testPlayerSet)
      assert(
        playerSet.contains("person1") &&
          playerSet.contains("person2") &&
          playerSet.contains("person3"))
    }

    "relay player set to the game" in new GameProbe {

      val testPlayerSet = Json.parse("{\"type\":\"options\",\"persons\":{\"person1\":{\"id\":2,\"name\":\"Person 1\",\"weapon1\":3,\"weapon2\":2},\"person2\":{\"id\":1,\"name\":\"Person 2\",\"weapon1\":2,\"weapon2\":2},\"person3\":{\"id\":2,\"name\":\"Person 3\",\"weapon1\":2,\"weapon2\":2}}}")
      userActor.game = gameProbe.ref
      userActor.receive(testPlayerSet)

      val playerSet = Person.toPersons(testPlayerSet)
      gameProbe.expectMsg(PlayerSet(SessionSpec.testUser.id.get, playerSet))
    }

    "toTurnSet must translate json to player movements at the game turn" in new GameProbe {
      val turnSet = GamingSet.toTurnSet(SessionSpec.testTurnSet)
      assert(
        turnSet.movements.contains("person1") &&
          turnSet.movements.contains("person2") &&
          turnSet.movements.contains("person3") &&
        turnSet.weapons.contains("person1") &&
          turnSet.weapons.contains("person2") &&
          turnSet.weapons.contains("person3"))
    }

    "relay turn set to the game" in new GameProbe {

      val testTurnSet = Json.parse("{\"type\":\"input\",\"persons\":{\"person1\":{\"x\":0,\"y\":0}}}")
      userActor.game = gameProbe.ref
      userActor.receive(testTurnSet)

      val turnSet = GamingSet.toTurnSet(testTurnSet)
      gameProbe.expectMsgClass(classOf[PlayerTurnSet])
    }
  }

}
