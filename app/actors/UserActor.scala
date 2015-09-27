package actors

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.LoggingReceive
import game.models.GamingSet
import models.{Person, User}
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.duration.Duration
import scala.xml.Utility

class UserActor(user: User, out: ActorRef) extends Actor with ActorLogging {

  val system = ActorSystem("UserActor")

  var game: ActorRef = null
  var board: ActorRef = BoardActor()
  var pingTimer : Cancellable = null

  override def preStart() = {
    board ! Subscribe(user)
    self ! Ping
  }

  def ping() = {
    import system.dispatcher

    pingTimer = system.scheduler.scheduleOnce(
      Duration.create(5, TimeUnit.SECONDS),
      self,
      Ping)
  }

  def receive = LoggingReceive {
    case command:JsValue if (command \ "type").asOpt[String].isDefined =>

      val t = (command \ "type").as[String]
      t match {
        case "message" =>
          val message: String = Utility.escape((command \ "content").as[String])
          if (message.length <= 140) {
            board ! Message(user, message)
          }
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
          game ! ReadyToTurn(user.id.get)
        case "input" if game != null =>
          val input = GamingSet.toTurnSet(command)
          if (input != null) {
            game ! PlayerTurnSet(user.id.get, input)
          }
        case "pong" =>
          self ! Pong
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

    case Ping =>
      out ! Json.obj("type" -> "ping")
      ping()

    case Pong =>
      log.debug("== Client pong ==")

    case other => log.error("== Unhandled :: " + other + "==")
  }
}

object UserActor {
  def props(user: User)(out: ActorRef) = Props(new UserActor(user, out))
}

object Ping
object Pong
