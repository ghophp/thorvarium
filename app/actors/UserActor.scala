package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.actor.ActorRef
import akka.actor.Props
import models.User
import play.api.libs.json.{JsValue, Json}

import scala.xml.Utility

class UserActor(user: User, out: ActorRef) extends Actor with ActorLogging {

  var game: ActorRef = null
  var board: ActorRef = BoardActor()

  override def preStart() = {
     board ! Subscribe(user)
  }

  def receive = LoggingReceive {
    case command:JsValue if (command \ "type").asOpt[String].isDefined =>

      val t = (command \ "type").as[String]
      t match {
        case "message" =>
          board ! Message(user, Utility.escape((command \ "content").as[String]))
        case "invitation" =>
          board ! Invitation(user, (command \ "to").as[Long])
        case "accept" =>
          board ! Accept(user, (command \ "from").as[Long])
        case other => log.error("Unhandled :: " + other)
      }

    case message:Message if sender == board =>
      out ! Json.obj(
        "type" -> "message",
        "content" -> message.message,
        "user" -> message.user.id)

    case invitation:Invitation if sender == board =>
      out ! Json.obj(
        "type" -> "invitation",
        "from" -> invitation.from.id)

    case s:StartGame =>
      game = sender()
      out ! Json.obj(
        "type" -> "game",
        "id" -> s.id,
        "players" -> s.players.map(u => u.toJson),
        "persons" -> s.persons.map { _.toJson },
        "weapons" -> s.weapons.map { _.toJson },
        "now" -> s.now)

    case NothingSelected if sender == game =>
      endGame()
      out ! Json.obj("type" -> "nothing_selected")

    case Won =>
      endGame()
      out ! Json.obj("type" -> "won")

    case Lose =>
      endGame()
      out ! Json.obj("type" -> "lose")

    case BoardMembers(members) if sender == board =>
      out ! Json.obj(
        "type" -> "members",
        "value" -> members)

    case other => log.error("Unhandled :: " + other)
  }

  def endGame() = {
    game = null
    board ! Subscribe(user)
  }
}

object UserActor {
  def props(user: User)(out: ActorRef) = Props(new UserActor(user, out))
}
