package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.actor.ActorRef
import akka.actor.Props
import models.{Weapon, Person, Player, User}
import play.api.libs.json.{JsObject, JsValue, Json}

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
          val persons = toPersons(command)
          if (persons.size >= 3) {
            game ! PlayerSet(user.id.get, persons)
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
        "players" -> s.players.map(u => u.toJson),
        "persons" -> s.persons.map { _.toJson },
        "weapons" -> s.weapons.map { _.toJson },
        "now" -> s.now)

    case r:GameReady =>
      out ! Json.obj(
        "type" -> "game_ready",
        "players" -> r.players.map(u => u.toJson),
        "now" -> r.now)

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

    case other => log.error("== Unhandled :: " + other + "==")
  }

  /**
   * Sample user data
   * {
   *  "persons": {
   *    "person1": {
   *      "id": 1,
   *      "weapon1": 1,
   *      "weapon2": 2
   *    },
   *    "person2": {
   *      "id": 1,
   *      "weapon1": 1,
   *      "weapon2": 2
   *    },
   *    "person3": {
   *      "id": 1,
   *      "weapon1": 1,
   *      "weapon2": 2
   *    }
   *  }
   * }
   * @param command user json
   * @return mapped player options
   */
  def toPersons(command : JsValue) : Map[String, Person] = {

    val personsJs = (command \ "persons").asOpt[JsObject] match {
      case Some(j) => j.fieldSet
        .filter( p => Player.PersonSlots.contains(p._1) )
        .filter( p => p._2.asOpt[JsObject].getOrElse(Json.obj()).fieldSet.count( w => Person.WeaponSlots.contains(w._1) ) >= 2 )
      case None => Set.empty
    }

    if (personsJs.size >= 3) {

      val persons = personsJs.map { js =>
        val p = Person.findBy( (js._2 \ "id").asOpt[Long].getOrElse(0))
        if (p.size > 0) {
          js._1 -> p(0)
        } else js._1 -> null
      }.filter( p => p._2 != null ).toMap

      if (persons.size >= 3) {

        val weapons = personsJs.map { js =>
          val w1 = Weapon.findBy( (js._2 \ Person.WeaponSlot1).asOpt[Long].getOrElse(0))
          val w2 = Weapon.findBy( (js._2 \ Person.WeaponSlot2).asOpt[Long].getOrElse(0))
          if (w1.size > 0 && w2.size > 0) {
            (js._2 \ "id").as[Long] -> Map[String, Weapon](Person.WeaponSlot1 -> w1(0), Person.WeaponSlot2 -> w2(0))
          } else (js._2 \ "id").as[Long] -> Map.empty[String, Weapon]
        }.toMap

        if (weapons.count( w => w._2.size >= 2 ) >= 3) {
          return persons.map { p => p._2.weapons = weapons(p._2.id.get); p }
        }
      }
    }

    Map.empty
  }

  def endGame() = {
    game = null
    board ! Subscribe(user)
  }
}

object UserActor {
  def props(user: User)(out: ActorRef) = Props(new UserActor(user, out))
}
