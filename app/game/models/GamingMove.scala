package game.models

import play.api.libs.json.Json

class GamingMove(var x: Double, var y: Double) {
  def toJson = {
    Json.obj("x" -> x, "y" -> y)
  }
}
