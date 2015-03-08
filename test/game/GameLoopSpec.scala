package game

import _root_.models.{Person, Player}
import game.models.GamingSet
import integration.WithTestDatabase
import org.scalatest.mock.MockitoSugar
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.test.PlaySpecification
import session.SessionSpec

class GameLoopSpec extends PlaySpecification with WithTestDatabase with MockitoSugar {

  trait GameLoopData extends Scope {

    val player1 = new Player(SessionSpec.testUser, Player.Player1)
    player1.persons = Person.toPersons(SessionSpec.testPlayerSet)

    val player2 = new Player(SessionSpec.testUser2, Player.Player2)
    player2.persons = Person.toPersons(SessionSpec.testPlayerSet)

    val testTurnSet = Json.obj("persons" -> Json.obj("person1" -> Json.obj("x" -> 103, "y" -> 103)))
    val testDistanceTurnSet = Json.obj("persons" -> Json.obj("person1" -> Json.obj("x" -> 400, "y" -> 400)))
  }

  "GameLoopTest" should {
    "must start with player at their positions" in new GameLoopData {

      val gameTest = new GameLoop(Set(player1, player2))

      val p1 = gameTest.players.find( _.slot == Player.Player1 ).get
      val p2 = gameTest.players.find( _.slot == Player.Player2 ).get

      p1.persons(Player.PersonSlot1).x must beEqualTo(20)
      p1.persons(Player.PersonSlot1).y must beEqualTo(20)
      p2.persons(Player.PersonSlot1).x must beEqualTo(450)
      p2.persons(Player.PersonSlot1).y must beEqualTo(450)
    }

    "must move ship to the point and not allow " in new GameLoopData {

      val gameTest = new GameLoop(Set(player1, player2))

      var p1 = gameTest.players.find( _.slot == Player.Player1 ).get
      p1.input = GamingSet.toTurnSet(testTurnSet)

      gameTest.state = GameLoop.Running
      var lastTime = System.currentTimeMillis()

      while (gameTest.state == GameLoop.Running) {
        val current = System.currentTimeMillis()

        gameTest.reset()
        gameTest.update((current - lastTime) / 1000)

        lastTime = current
      }

      p1.persons(Player.PersonSlot1).x.toInt must beEqualTo(100)
      p1.persons(Player.PersonSlot1).y.toInt must beEqualTo(100)
    }

    "must not move ship to a point that overcome the max distance of the person" in new GameLoopData {

      val gameTest = new GameLoop(Set(player1, player2))
      var p1 = gameTest.players.find( _.slot == Player.Player1 ).get

      val person1 = p1.persons(Player.PersonSlot1)
      val maxDistance = (GameLoop.MaxDistance / 100) * person1.distance

      p1.input = GamingSet.toTurnSet(testDistanceTurnSet)
      p1.input.movements(Player.PersonSlot1).x must beEqualTo(20 + maxDistance)
      p1.input.movements(Player.PersonSlot1).y must beEqualTo(20 + maxDistance)
    }
  }
}
