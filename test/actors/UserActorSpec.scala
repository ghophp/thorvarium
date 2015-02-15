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
    val boardProbe = new TestProbe(system)
    val socketActorProbe = new TestProbe(system)

    "relay messages to the board, along with its ID" in new WithApplication {

      val userActorRef = TestActorRef[UserActor](Props(classOf[UserActor], SessionSpec.testUser, boardProbe.ref, socketActorProbe.ref))
      val userActor = userActorRef.underlyingActor

      val text = "test message"
      val testMsg = Json.obj("type" -> "message", "content" -> text)

      userActor.receive(testMsg)

      boardProbe.expectMsg(Subscribe(SessionSpec.testUser))
      boardProbe.expectMsg(Message(SessionSpec.testUser, text))
    }
  }

}
