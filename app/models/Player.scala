package models

import play.api.libs.json.Json

case class Player(user: User, persons : Map[String, Person]) {
  def toJson = {
    Json.obj(
      "user" -> user.toJson,
      "persons" -> Json.arr( persons.map { _._2.toJson } ))
  }
}

object Player {

}
