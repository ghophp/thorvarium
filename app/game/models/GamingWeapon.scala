package game.models

import play.api.libs.json.Json

class GamingWeapon(val x : Double, val y : Double) {
  def toJson = {
    Json.obj("x" -> x, "y" -> y)
  }
}
