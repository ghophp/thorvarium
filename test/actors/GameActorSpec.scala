package actors

import akka.actor.{PoisonPill, Props}
import akka.testkit.{TestActorRef, TestProbe}
import integration.WithTestDatabase
import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationLike
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication
import session.SessionSpec

@RunWith(classOf[JUnitRunner])
class GameActorSpec extends AbstractTestKit("GameActorSpec") with SpecificationLike with WithTestDatabase {

  val testGameId = SessionSpec.testUser.id.get + "-" + SessionSpec.testUser2.id.get

  trait GameProbe extends WithApplication {

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

  "GameActor" should {

    "have weapons and persons at start" in new WithApplication with GameProbe {
      assert(gameActor.persons.size == 3)
      assert(gameActor.weapons.size == 3)
    }

    "should inform users on game get two players" in new WithApplication with GameProbe {

      assert(gameActor.players.size == 0)

      gameActorRef ! SubscribeGame(userActorRef, SessionSpec.testUser)
      gameActorRef ! SubscribeGame(userActorRef2, SessionSpec.testUser2)

      awaitCond(gameActor.players.size == 2)

      userActor.game mustNotEqual null
      userActor2.game mustNotEqual null
    }

    "one of the users should win the game in case of the other lose connection" in new WithApplication with GameProbe {

      assert(gameActor.players.size == 0)

      gameActorRef ! SubscribeGame(probe1.ref, SessionSpec.testUser)
      gameActorRef ! SubscribeGame(probe2.ref, SessionSpec.testUser2)

      probe1.ref ! PoisonPill

      probe2.expectMsgClass(classOf[StartGame])
      probe2.expectMsg(Won)
    }
  }

}
