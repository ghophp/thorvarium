package actors

import java.util.concurrent.TimeUnit

import akka.actor.{PoisonPill, Props}
import akka.testkit.{TestActorRef, TestProbe}
import integration.WithTestDatabase
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import session.SessionSpec

import scala.concurrent.duration.Duration

class GameActorSpec extends AbstractTestKit("GameActorSpec") with SpecificationLike with WithTestDatabase {

  val testGameId = SessionSpec.testUser.id.get + "-" + SessionSpec.testUser2.id.get

  trait GameProbe extends Scope {

    val socketActorProbe = new TestProbe(system)

    val probe1 = new TestProbe(system)
    val probe2 = new TestProbe(system)

    val userActorRef = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser, socketActorProbe.ref))
    val userActor = userActorRef.underlyingActor

    val userActorRef2 = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser2, socketActorProbe.ref))
    val userActor2 = userActorRef2.underlyingActor

    val gameActorRef = TestActorRef[GameActor](Props(classOf[GameActor], testGameId))
    val gameActor = gameActorRef.underlyingActor
  }

  trait PlayGameProbe extends GameProbe {

    val personsTest = Map[String, Long](
      "person1" -> 1,
      "person2" -> 2,
      "person3" -> 3)

    val weaponsTest = Map[String, Map[String, Long]](
      "person1" -> Map("weapon1" -> 1, "weapon2" -> 2),
      "person2" -> Map("weapon1" -> 1, "weapon2" -> 2),
      "person3" -> Map("weapon1" -> 1, "weapon2" -> 2))
  }

  "GameActor" should {

    "should inform users when game get two players" in new GameProbe {

      assert(gameActor.players.size == 0)

      gameActorRef ! SubscribeGame(SessionSpec.testUser, userActorRef)
      gameActorRef ! SubscribeGame(SessionSpec.testUser2, userActorRef2)

      awaitCond(gameActor.players.size == 2)

      userActor.game mustNotEqual null
      userActor2.game mustNotEqual null
    }

    "one of the users should win the game in case of the other lose connection" in new GameProbe {

      assert(gameActor.players.size == 0)

      gameActorRef ! SubscribeGame(SessionSpec.testUser, probe1.ref)
      gameActorRef ! SubscribeGame(SessionSpec.testUser2, probe2.ref)

      probe1.ref ! PoisonPill

      probe2.expectMsgClass(classOf[StartGame])
      probe2.expectMsg(Won)
    }

    "if in 40 seconds both players have not selected persons and weapons the game end" in new GameProbe {

      assert(gameActor.players.size == 0)

      gameActorRef ! SubscribeGame(SessionSpec.testUser, probe1.ref)
      gameActorRef ! SubscribeGame(SessionSpec.testUser2, probe2.ref)

      probe2.expectMsgClass(classOf[StartGame])
      probe2.expectMsg(Duration.create(50, TimeUnit.SECONDS), NothingSelected)
    }

    "if both selected, stop the timer and proceed to game loop" in new PlayGameProbe {

      assert(gameActor.players.size == 0)

      gameActorRef ! SubscribeGame(SessionSpec.testUser, probe1.ref)
      gameActorRef ! SubscribeGame(SessionSpec.testUser2, probe2.ref)

      Thread.sleep(3000)

      gameActorRef ! PlayerSet(SessionSpec.testUser.id.get, personsTest, weaponsTest)
      gameActorRef ! PlayerSet(SessionSpec.testUser2.id.get, personsTest, weaponsTest)

      gameActor.stepTimer mustEqual null
    }
  }

}
