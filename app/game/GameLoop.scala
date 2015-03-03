package game

import game.models.GamingPlayer

class GameLoop(var player1 : GamingPlayer, var player2: GamingPlayer) {

  var state = GameLoop.WaitingInput
  var steps = 0
  var turns = 0

  def reset() = {
    steps = 0
  }

  def update(elapsed : Long) = {
    if (player1.input != null || player2.input != null) {

      if (player1.input != null) {
        applyMovement(player1, elapsed)
      }

      if (player2.input != null) {
        applyMovement(player2, elapsed)
      }
    }

    if (steps <= 0) {
      state = GameLoop.WaitingInput
    }
  }

  def applyMovement(p: GamingPlayer, elapsed : Long) = {
    if (p.input.movements != null) {

      p.input.movements.map { m =>
        val person = p.player.persons(m._1)
        if (person.x != m._2.x || person.y != m._2.y) {

          val angle = Math.atan2(m._2.x - person.x, m._2.y - person.y)
          person.x += Math.sin(angle) * ((GameLoop.MaxSpeed / 100) * person.speed) * elapsed
          person.y += Math.cos(angle) * ((GameLoop.MaxSpeed / 100) * person.speed) * elapsed

          steps += 1
          println("== " + m._1 + " move to " + person.x + "," + person.y + " ==")
        }
      }
    }
  }
}

object GameLoop {
  val WaitingInput = 1
  val Running = 2

  val sceneWidth = 500
  val sceneHeight = 500

  val MaxSpeed = 2 // 2 pixels per second
}
