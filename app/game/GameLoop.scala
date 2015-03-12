package game

import _root_.models.{Person, Weapon, Player}
import game.models.GamingBullet

import scala.util.control.Breaks._

class GameLoop(var players : Set[Player]) {

  var state = GameLoop.WaitingInput
  var steps = 0
  var turns = 0

  var bullets = Set[GamingBullet]()
  var collisions = Set[GamingBullet]()

  def loop() = {

    state = GameLoop.Running
    parseWeapons()

    var lastTime = System.currentTimeMillis()
    while (state == GameLoop.Running) {

      val current = System.currentTimeMillis()
      val delta = current - lastTime

      breakable {
        if (delta <= 0.0) {
          break()
        }

        reset()
        update(delta.toDouble / 1000.0)

        lastTime = current
      }
    }

    if (players.count( p => p.persons.count(_._2.life > 0) > 0 ) < 2) {
      state = GameLoop.Ended
    } else {
      newTurn()
    }
  }

  def winner() : Long = {
    players.filter( p => p.persons.count(_._2.life > 0) > 0 ).toList(0).user.id.get
  }

  def draw() : Boolean = {
    players.count( p => p.persons.count(_._2.life > 0) > 0 ) <= 0
  }

  def reset() = {
    steps = 0
  }

  def update(elapsed : Double) = {

    players.map { p =>
      applyCollisions(p.persons)
    }

    players.map { p =>
      if (p.input != null) {
        applyMovements(p, elapsed)
      }
    }

    applyBullets(elapsed)

    if (steps <= 0) {
      state = GameLoop.WaitingInput
    }
  }

  def applyMovements(p: Player, elapsed : Double) = {
    if (p.input.movements != null) {
      p.input.movements.map { m =>

        val person = p.persons(m._1)
        if (person.life > 0) {
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
  }

  def applyCollisions(persons: Map[String, Person]) = {
    persons.map { p =>
      bullets.map { b =>
        if (p._2 != b.person && b.collided(p._2)) {
          p._2.life -= b.power
          collisions += b
        }
      }
    }
    bullets --= collisions
  }

  def applyBullets(elapsed : Double) = {
    bullets.map { b =>
      val speed = b.speed * elapsed
      if (b.x > 0 && b.x < GameLoop.SceneWidth &&
        b.y > 0 && b.y < GameLoop.SceneHeight) {

        b.x = b.x + (Math.cos(b.angle) * speed)
        b.y = b.y + (Math.sin(b.angle) * speed)
        steps += 1
      }
    }
  }

  def parseWeapons() = {
    bullets = Set()
    players.map { p =>
      if (p.input != null && p.input.weapons != null) {
        p.input.weapons.map { m =>

          val person = p.persons(m._1)
          if (person.life > 0 && m._2 != null) {

            val w = m._2
            val weapon = person.weapons(w._1)
            val angle = Math.atan2(w._2.y - person.y, w._2.x - person.x)
            val speed = (GameLoop.MaxBulletSpeed / 100.0) * weapon.speed
            val power = (GameLoop.MaxBulletPower / 100.0) * weapon.power
            val size = (GameLoop.MaxBulletSize / 100.0) * weapon.size

            if (weapon.kind == Weapon.SingleShot) {

              bullets += new GamingBullet(p,
                person,
                weapon,
                angle,
                speed,
                power,
                size,
                person.x,
                person.y)

            } else if (weapon.kind == Weapon.TripleShot) {
              for (x <- List(-1, 0, 1)) {
                bullets += new GamingBullet(p,
                  person,
                  weapon,
                  angle + (x * GameLoop.TripleShotVariation),
                  speed,
                  power,
                  size,
                  person.x,
                  person.y)
              }
            }
          }
        }
      }
    }
  }

  def newTurn() = {
    turns += 1
    players.map { _.input = null }
    collisions = Set()
  }
}

object GameLoop {
  val WaitingInput = 1
  val Running = 2
  val Ended = 3

  val SceneWidth = 500
  val SceneHeight = 500

  val SceneGap = 20.0
  val SceneGapW = SceneWidth - SceneGap
  val SceneGapH = SceneHeight - SceneGap

  val MaxSpeed = 40.0 // 30 pixels per second
  val MaxDistance = 120.0
  val MaxSize = 17.0

  val MaxBulletSpeed = 200.0
  val MaxBulletPower = 25.0
  val MaxBulletSize = 5.0

  val TripleShotVariation = 0.06
}
