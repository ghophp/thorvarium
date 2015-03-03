package game

import _root_.models.{Person, Player}
import game.models.{GamingSet, GamingPlayer}
import integration.WithTestDatabase
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.PlaySpecification
import session.SessionSpec

class GameLoopSpec extends PlaySpecification with WithTestDatabase with MockitoSugar {

  class GameLoopTest extends GameLoop(
    GameLoopSpec.gamingPlayer1,
    GameLoopSpec.gamingPlayer2)

  "GameLoopTest" should {
    "must start with player at their positions" in {

      val gameTest = new GameLoopTest

      gameTest.player1.player.persons(Player.PersonSlot1).x must beEqualTo(50)
      gameTest.player1.player.persons(Player.PersonSlot1).y must beEqualTo(50)
      gameTest.player2.player.persons(Player.PersonSlot1).x must beEqualTo(450)
      gameTest.player2.player.persons(Player.PersonSlot1).y must beEqualTo(450)
    }

    "must do nothing ships to the point" in {

      val gameTest = new GameLoopTest
      gameTest.player1.input = GamingSet.toTurnSet(GameLoopSpec.testTurnSet)

      var lastTime = System.currentTimeMillis()
      while (gameTest.state == GameLoop.Running) {
        val current = System.currentTimeMillis()

        gameTest.reset()
        gameTest.update(current - lastTime)

        lastTime = current
      }

      gameTest.player1.player.persons(Player.PersonSlot1).x must beEqualTo(100)
      gameTest.player1.player.persons(Player.PersonSlot1).y must beEqualTo(100)
    }
  }
}

object GameLoopSpec extends GameLoopSpec {
  val player1 = new Player(SessionSpec.testUser, Person.toPersons(SessionSpec.testPlayerSet))
  val player2 = new Player(SessionSpec.testUser2, Person.toPersons(SessionSpec.testPlayerSet))

  val gamingPlayer1 = new GamingPlayer(player1, GamingPlayer.Player1, null)
  val gamingPlayer2 = new GamingPlayer(player2, GamingPlayer.Player2, null)

  val testTurnSet = Json.obj("persons" -> Json.obj("person1" -> Json.obj("x" -> 100, "y" -> 100)))
}
