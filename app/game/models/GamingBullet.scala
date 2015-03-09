package game.models

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

}
