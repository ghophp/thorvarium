package game.models

import game.GameLoop
import models.Player

class GamingPlayer(val player: Player, val slot: Int) {

  private var _input: GamingSet = null

  val Player1Start = Map[String, Map[String, Float]](
    Player.PersonSlot1 -> Map("x" -> 50f, "y" -> 50f),
    Player.PersonSlot2 -> Map("x" -> 50f, "y" -> 150f),
    Player.PersonSlot3 -> Map("x" -> 150f, "y" -> 50f))

  val Player2Start = Map[String, Map[String, Float]](
    Player.PersonSlot1 -> Map("x" -> 450f, "y" -> 450f),
    Player.PersonSlot2 -> Map("x" -> 450f, "y" -> 400f),
    Player.PersonSlot3 -> Map("x" -> 400f, "y" -> 450f))

  val startPosition = if (slot == GamingPlayer.Player1) { Player1Start } else { Player2Start }
  player.persons.map { p =>
    val slot = startPosition(p._1)
    p._2.x = slot("x")
    p._2.y = slot("y")
  }

  def input = _input
  def input_= (set:GamingSet):Unit = {
    if (set != null && set.movements != null) {
      set.movements.map { m =>

        val person = player.persons(m._1)
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
}

object GamingPlayer {
  val Player1 = 1
  val Player2 = 2
}
