package models

import play.api.libs.json.Json

case class Player(user: User, var persons : Map[String, Person] = Map.empty) {
  def toJson = {
    Json.obj(
      "user" -> user.toJson,
      "persons" -> persons.map { _._2.toJson })
  }
}

object Player {

  val PersonSlot1 = "person1"
  val PersonSlot2 = "person2"
  val PersonSlot3 = "person3"

  val PersonSlots = Set[String](PersonSlot1, PersonSlot2, PersonSlot3)
}
