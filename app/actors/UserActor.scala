package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.actor.ActorRef
import akka.actor.Props
import play.api.libs.json.Json


class UserActor(uuid: String, board: ActorRef, out: ActorRef) extends Actor with ActorLogging {

  override def preStart() = {
    BoardActor() ! Subscribe
  }

  def receive = LoggingReceive {
    case BoardMembers(members) if sender == board =>
      out ! Json.obj("command" -> "members", "value" -> members)
    case other => log.error(">>> Unhandled: " + other)
  }
}

object UserActor {
  def props(uuid: String)(out: ActorRef) = Props(new UserActor(uuid, BoardActor(), out))
}
