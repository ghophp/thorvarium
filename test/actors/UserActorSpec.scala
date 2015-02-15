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

  "UserActor" should {

    "relay messages to the board" in new WithApplication {

      val boardProbe = new TestProbe(system)
      val socketActorProbe = new TestProbe(system)

      val userActorRef = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser, boardProbe.ref, socketActorProbe.ref))
      val userActor = userActorRef.underlyingActor

      val text = "test message"
      val testMsg = Json.obj("type" -> "message", "content" -> text)

      userActor.receive(testMsg)
      boardProbe.expectMsg(Message(SessionSpec.testUser, text))
    }

    "relay invitation to the board" in new WithApplication {

      val boardProbe = new TestProbe(system)
      val socketActorProbe = new TestProbe(system)

      val userActorRef = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser, boardProbe.ref, socketActorProbe.ref))
      val userActor = userActorRef.underlyingActor

      val userActorRef2 = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser2, boardProbe.ref, socketActorProbe.ref))
      val userActor2 = userActorRef2.underlyingActor
      val testInvite = Json.obj("type" -> "invitation", "to" -> SessionSpec.testUser2.id)

      userActor.receive(testInvite)
      boardProbe.expectMsg(Invitation(SessionSpec.testUser, SessionSpec.testUser2.id.get))
    }

    "relay accept invitation to the board" in new WithApplication {

      val boardProbe = new TestProbe(system)
      val socketActorProbe = new TestProbe(system)

      val userActorRef = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser, boardProbe.ref, socketActorProbe.ref))
      val userActor = userActorRef.underlyingActor

      val userActorRef2 = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser2, boardProbe.ref, socketActorProbe.ref))
      val userActor2 = userActorRef2.underlyingActor

      val testInvite = Json.obj("type" -> "invitation", "to" -> SessionSpec.testUser2.id)
      val testAccept = Json.obj("type" -> "accept", "from" -> SessionSpec.testUser.id)

      userActor.receive(testInvite)
      userActor2.receive(testAccept)

      boardProbe.expectMsg(Accept(SessionSpec.testUser2, SessionSpec.testUser.id.get))
    }
  }

}
