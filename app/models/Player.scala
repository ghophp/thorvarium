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
    Player.PersonSlot1 -> Map("x" -> 480.0, "y" -> 480.0),
    Player.PersonSlot2 -> Map("x" -> 480.0, "y" -> 430.0),
    Player.PersonSlot3 -> Map("x" -> 430.0, "y" -> 480.0))

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
        val distX = person.x - m._2.x
        val distY = person.y - m._2.y

        if (Math.abs(distX) > max) {
          if (distX < 0) {
            m._2.x = person.x + max
          } else {
            m._2.x = person.x - max
          }
        }
        if (Math.abs(distY) > max) {
          if (distY < 0) {
            m._2.y = person.y + max
          } else {
            m._2.y = person.y - max
          }
        }

        if (m._2.x < GameLoop.SceneGap || m._2.x > GameLoop.SceneGapW) {
          m._2.x =
            if (m._2.x < GameLoop.SceneGap)
              GameLoop.SceneGap
            else
              GameLoop.SceneGapW
        }
        if (m._2.y < GameLoop.SceneGap || m._2.y > GameLoop.SceneGapH) {
          m._2.y =
            if (m._2.y < GameLoop.SceneGap)
              GameLoop.SceneGap
            else
              GameLoop.SceneGapH
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
