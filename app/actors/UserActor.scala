package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.actor.ActorRef
import akka.actor.Props
import game.models.GamingSet
import models.{Person, User}
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
        case "options" if game != null =>
          val persons = Person.toPersons(command)
          if (persons.size >= 3) {
            game ! PlayerSet(user.id.get, persons)
          }
        case "ready_to_turn" if game != null =>
          game ! ReadyToTurn
        case "input" if game != null =>
          val input = GamingSet.toTurnSet(command)
          if (input != null) {
            game ! PlayerTurnSet(user.id.get, input)
          }
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
        "persons" -> s.persons.map { _.toJson },
        "weapons" -> s.weapons.map { _.toJson },
        "now" -> s.now)

    case r:GameReady if sender == game =>
      out ! Json.obj(
        "type" -> "game_ready",
        "players" -> r.players.map(u => u.toJson),
        "now" -> r.now)

    case r:PreTurn if sender == game =>
      out ! Json.obj(
        "type" -> "pre_turn",
        "inputs" -> Json.toJson(r.inputs.map { p =>
          p._1 -> (if (p._2 != null) p._2.toJson else Json.obj())
        }))

    case r:AfterTurn if sender == game =>
      out ! Json.obj(
        "type" -> "after_turn",
        "players" -> r.players.map(u => u.toJson),
        "turns" -> r.turns)

    case TurnStart if sender == game =>
      out ! Json.obj("type" -> "turn_start")

    case NothingSelected if sender == game =>
      game = null
      out ! Json.obj("type" -> "nothing_selected")

    case Won =>
      game = null
      out ! Json.obj("type" -> "won")

    case Lose =>
      game = null
      out ! Json.obj("type" -> "lose")

    case BoardMembers(members) if sender == board =>
      out ! Json.obj(
        "type" -> "members",
        "value" -> members)

    case other => log.error("== Unhandled :: " + other + "==")
  }
}

object UserActor {
  def props(user: User)(out: ActorRef) = Props(new UserActor(user, out))
}
