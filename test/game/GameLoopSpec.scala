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

    val testTurnSet = Json.obj("persons" -> Json.obj("person1" -> Json.obj("x" -> 100, "y" -> 100)))
    val testDistanceTurnSet = Json.obj("persons" -> Json.obj("person1" -> Json.obj("x" -> 400, "y" -> 400)))
    val testInverseTurnSet = Json.obj("persons" -> Json.obj("person1" -> Json.obj("x" -> 20, "y" -> 20)))

    val testBoardTurnSet = Json.obj("persons" -> Json.obj("person1" -> Json.obj("x" -> 5, "y" -> 5)))
    val testBoardInvertTurnSet = Json.obj("persons" -> Json.obj("person1" -> Json.obj("x" -> 495, "y" -> 495)))

    val testHitTurnSet = Json.obj("persons" -> Json.obj("person1" ->
      Json.obj("x" -> 25, "y" -> 25, "weapon1" -> Json.obj("x" -> 25, "y" -> 25))))
  }

  "GameLoopTest" should {
    "must start with player at their positions" in new GameLoopData {

      val gameTest = new GameLoop(Set(player1, player2))

      val p1 = gameTest.players.find( _.slot == Player.Player1 ).get
      val p2 = gameTest.players.find( _.slot == Player.Player2 ).get

      p1.persons(Player.PersonSlot1).x must beEqualTo(20)
      p1.persons(Player.PersonSlot1).y must beEqualTo(20)
      p2.persons(Player.PersonSlot1).x must beEqualTo(480)
      p2.persons(Player.PersonSlot1).y must beEqualTo(480)
    }

    "must move ship to the point" in new GameLoopData {

      val gameTest = new GameLoop(Set(player1, player2))

      var p1 = gameTest.players.find( _.slot == Player.Player1 ).get
      p1.input = GamingSet.toTurnSet(testTurnSet)

      gameTest.loop()

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

      // Inverse

      var p2 = gameTest.players.find( _.slot == Player.Player2 ).get
      val person2 = p2.persons(Player.PersonSlot1)
      val maxDistance2 = (GameLoop.MaxDistance / 100) * person2.distance

      p2.input = GamingSet.toTurnSet(testInverseTurnSet)
      p2.input.movements(Player.PersonSlot1).x must beEqualTo(480 - maxDistance)
      p2.input.movements(Player.PersonSlot1).y must beEqualTo(480 - maxDistance)
    }

    "must not move ship to minimum board gap" in new GameLoopData {

      val gameTest = new GameLoop(Set(player1, player2))

      var p1 = gameTest.players.find( _.slot == Player.Player1 ).get
      val person1 = p1.persons(Player.PersonSlot1)
      val maxDistance = (GameLoop.MaxDistance / 100) * person1.distance

      p1.input = GamingSet.toTurnSet(testBoardTurnSet)
      p1.input.movements(Player.PersonSlot1).x must beEqualTo(GameLoop.SceneGap)
      p1.input.movements(Player.PersonSlot1).y must beEqualTo(GameLoop.SceneGap)

      // Inverse

      var p2 = gameTest.players.find( _.slot == Player.Player2 ).get
      val person2 = p2.persons(Player.PersonSlot1)
      val maxDistance2 = (GameLoop.MaxDistance / 100) * person2.distance

      p2.input = GamingSet.toTurnSet(testBoardInvertTurnSet)
      p2.input.movements(Player.PersonSlot1).x must beEqualTo(GameLoop.SceneGapW)
      p2.input.movements(Player.PersonSlot1).y must beEqualTo(GameLoop.SceneGapH)
    }

    "must hit player ship" in new GameLoopData {

      val gameTest = new GameLoop(Set(player1, player2))

      var p1 = gameTest.players.find( _.slot == Player.Player1 ).get
      p1.input = GamingSet.toTurnSet(testHitTurnSet)

      gameTest.loop()

      var p2 = gameTest.players.find( _.slot == Player.Player2 ).get
      p2.persons(Player.PersonSlot1).life must beEqualTo(25)
    }
  }
}
