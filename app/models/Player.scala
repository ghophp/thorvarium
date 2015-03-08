package models

import game.GameLoop
import game.models.GamingSet
import play.api.libs.json.Json

case class Player(user: User,  slot: Int) {

  private var _input: GamingSet = null
  private var _persons: Map[String, Person] = Map.empty

  val Player1Start = Map[String, Map[String, Double]](
    Player.PersonSlot1 -> Map("x" -> 20.0, "y" -> 20.0),
    Player.PersonSlot2 -> Map("x" -> 20.0, "y" -> 70.0),
    Player.PersonSlot3 -> Map("x" -> 70.0, "y" -> 20.0))

  val Player2Start = Map[String, Map[String, Double]](
    Player.PersonSlot1 -> Map("x" -> 450.0, "y" -> 450.0),
    Player.PersonSlot2 -> Map("x" -> 450.0, "y" -> 400.0),
    Player.PersonSlot3 -> Map("x" -> 400.0, "y" -> 450.0))

  val startPosition = if (slot == Player.Player1) { Player1Start } else { Player2Start }

  def toJson = {
    Json.obj(
      "user" -> user.toJson,
      "slot" -> slot,
      "persons" -> Json.toJson( persons.map( p => p._1 -> p._2.toJson ) ))
  }

  def input = _input
  def input_= (set:GamingSet):Unit = {
    if (set != null && set.movements != null) {
      set.movements.map { m =>

        val person = persons(m._1)
        val max = (GameLoop.MaxDistance / 100) * person.distance

        if (m._2.x > person.x + max) {
          m._2.x = person.x + max
        }
        if (m._2.y > person.y + max) {
          m._2.y = person.x + max
        }
      }
    }

    _input = set
  }

  def persons = _persons
  def persons_= (set:Map[String, Person]):Unit = {
    if (set != null && set.size > 0) {
      set.map { p =>
        val slot = startPosition(p._1)
        p._2.x = slot("x")
        p._2.y = slot("y")
      }
    }

    _persons = set
  }
}

object Player {

  val Player1 = 1
  val Player2 = 2

  val PersonSlot1 = "person1"
  val PersonSlot2 = "person2"
  val PersonSlot3 = "person3"

  val PersonSlots = Set[String](PersonSlot1, PersonSlot2, PersonSlot3)
}
