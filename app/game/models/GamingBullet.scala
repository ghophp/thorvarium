package game.models

import game.GameLoop
import models.{Person, Weapon, Player}

class GamingBullet(val player : Player,
                   val person : Person,
                   val weapon : Weapon,
                   val angle : Double,
                   val speed : Double,
                   val power : Double,
                   val size : Double,
                   var x : Double,
                   var y : Double) {

  def collided(p: Person) : Boolean = {
    val pSize = (GameLoop.MaxSize / 100.0) * p.size
    val dx = p.x - x
    val dy = p.y - y
    val rs = size + pSize
    dx*dx+dy*dy <= rs*rs
  }
}
