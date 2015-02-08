package actors

import scala.concurrent.duration.DurationInt
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

  "BoardActor" should {
    val probe1 = new TestProbe(system)
    val probe2 = new TestProbe(system)

    "accept subscriptions" in new WithApplication {
      val boardActorRef = TestActorRef[BoardActor](Props[BoardActor])
      val boardActor = boardActorRef.underlyingActor

      assert(boardActor.users.size == 0)

      probe1.send(boardActorRef, Subscribe)
      probe2.send(boardActorRef, Subscribe)

      awaitCond(boardActor.users.size == 2, 100 millis)

      assert(boardActor.users.contains(probe1.ref))
      assert(boardActor.users.contains(probe2.ref))

    }

    "and broadcast messages" in new WithApplication {
      val boardActorRef = TestActorRef[BoardActor](Props[BoardActor])
      val boardActor = boardActorRef.underlyingActor

      probe1.send(boardActorRef, Subscribe)
      probe2.send(boardActorRef, Subscribe)

      awaitCond(boardActor.users.size == 2, 100 millis)

      val msg = Message("sender", "test message")
      boardActorRef.receive(msg)
      probe1.expectMsg(msg)
      probe2.expectMsg(msg)
    }
    
    "and watch its users" in new WithApplication {
      val boardActorRef = TestActorRef[BoardActor](Props[BoardActor])
      val boardActor = boardActorRef.underlyingActor

      probe1.send(boardActorRef, Subscribe)
      probe2.send(boardActorRef, Subscribe)

      awaitCond(boardActor.users.size == 2, 100 millis)
      
      probe2.ref ! PoisonPill
      
      awaitCond(boardActor.users.size == 1, 100 millis)
      
    }
  }

}