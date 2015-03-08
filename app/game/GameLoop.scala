package game

import _root_.models.Player

class GameLoop(var players : Set[Player]) {

  var state = GameLoop.WaitingInput
  var steps = 0
  var turns = 0

  def reset() = {
    steps = 0
  }

  def update(elapsed : Double) = {

    players.map { p =>
      if (p.input != null) {
        applyMovement(p, elapsed)
      }
    }

    if (steps <= 0) {
      state = GameLoop.WaitingInput
    }
  }

  def applyMovement(p: Player, elapsed : Double) = {
    if (p.input.movements != null) {

      p.input.movements.map { m =>

        val person = p.persons(m._1)
        val angle = Math.atan2(m._2.y - person.y, m._2.x - person.x)
        val speed = ((GameLoop.MaxSpeed / 100.0) * person.speed) * elapsed

        if (Math.abs(person.x - m._2.x) > 1) {
          person.x = person.x + (Math.cos(angle) * speed)
          steps += 1
        } else {
          person.x = m._2.x
        }

        if (Math.abs(person.y - m._2.y) > 1) {
          person.y = person.y + (Math.sin(angle) * speed)
          steps += 1
        } else {
          person.y = m._2.y
        }
      }
    }
  }

  def newTurn() = {
    turns += 1
    players.map { _.input = null }
  }
}

object GameLoop {
  val WaitingInput = 1
  val Running = 2

  val sceneWidth = 500
  val sceneHeight = 500

  val MaxSpeed = 30.0 // 2 pixels per second
  val MaxDistance = 120.0
}
