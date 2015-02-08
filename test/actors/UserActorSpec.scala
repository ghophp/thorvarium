package actors

import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationLike
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication
import akka.actor.Props
import akka.testkit.TestActorRef
import akka.testkit.TestProbe

import play.api.libs.json.Json

@RunWith(classOf[JUnitRunner])
class UserActorSpec extends AbstractTestKit("UserActorSpec") with SpecificationLike {

  "UserActor" should {
    val boardProbe = new TestProbe(system)
    val socketActorProbe = new TestProbe(system)
    val uid = "test"

    "relay messages to the board, along with its ID" in new WithApplication {
      val userActorRef = TestActorRef[UserActor](Props(classOf[UserActor], uid, boardProbe.ref, socketActorProbe.ref))
      val userActor = userActorRef.underlyingActor

      val text = "test message"

      val testMsg = Json.obj("msg" -> text)

      userActor.receive(testMsg)

      boardProbe.expectMsg(Message(uid, text))

    }

    "escape its message" in new WithApplication {
      val userActorRef = TestActorRef[UserActor](Props(classOf[UserActor], uid, boardProbe.ref, socketActorProbe.ref))
      val userActor = userActorRef.underlyingActor

      val testMsg = Json.obj("msg" -> "<b>&</b>")

      userActor.receive(testMsg)

      boardProbe.expectMsg(Message(uid, "&lt;b&gt;&amp;&lt;/b&gt;"))

    }

  }

  "UserActor" should {
    val boardProbe = new TestProbe(system)
    val socketActorProbe = new TestProbe(system)
    val uid = "test"

    "relay messages from the board to the websocket" in new WithApplication {
      val userActorRef = TestActorRef[UserActor](Props(new UserActor(uid, boardProbe.ref, socketActorProbe.ref)))
      val userActor = userActorRef.underlyingActor

      val text = "test message from board"

      val testMsg = Message(uid, text)

      boardProbe.send(userActorRef, testMsg)

      socketActorProbe.expectMsg(Json.obj("type" -> "message", "msg" -> text, "uid" -> uid))
    }

    "but not if they don't come from the board" in new WithApplication {
      val userActorRef = TestActorRef[UserActor](Props(new UserActor(uid, boardProbe.ref, socketActorProbe.ref)))
      val userActor = userActorRef.underlyingActor

      val testMsg = Message("sender", "test message not from board")

      userActorRef.receive(testMsg)

      socketActorProbe.expectNoMsg
    }
  }

}
