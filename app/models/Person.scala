package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json.{JsObject, JsValue, Json}

case class Person(id: Option[Long] = None,
                  name: String,
                  var life: Double,
                  speed: Int,
                  size: Int,
                  distance: Int,
                  var x: Double = 0,
                  var y: Double = 0,
                  var weapons : Map[String, Weapon] = Map.empty) {

  def toJson = {
    Json.obj(
      "id" -> id.get,
      "name" -> name,
      "life" -> life,
      "speed" -> speed,
      "size" -> size,
      "distance" -> distance,
      "x" -> x,
      "y" -> y,
      "weapons" -> Json.toJson( weapons.map( p => p._1 -> p._2.toJson ) ))
  }
}

object Person {

  val WeaponSlot1 = "weapon1"
  val WeaponSlot2 = "weapon2"

  val WeaponSlots = Set[String](WeaponSlot1, WeaponSlot2)

  private val parser : RowParser[Person] = {
    get[Option[Long]]("id") ~
        get[String]("name") ~
        get[Int]("life") ~
        get[Int]("speed") ~
        get[Int]("size") ~
        get[Int]("distance") map {
      case id ~ name ~ life ~ speed ~ size ~ distance =>
        Person(id, name, life, speed, size, distance, 0, 0, Map.empty)
    }
  }

  def list: List[Person] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM person").as(parser *)
    }
  }

  def findBy(id : Long): List[Person] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM person WHERE id = {id}")
          .on('id -> id)
          .as(parser *)
    }
  }

  /**
   * Sample user data
   * {
   *  "persons": {
   *    "person1": {
   *      "id": 1,
   *      "weapon1": 1,
   *      "weapon2": 2
   *    },
   *    "person2": {
   *      "id": 1,
   *      "weapon1": 1,
   *      "weapon2": 2
   *    },
   *    "person3": {
   *      "id": 1,
   *      "weapon1": 1,
   *      "weapon2": 2
   *    }
   *  }
   * }
   * @param command user json
   * @return mapped player options
   */
  def toPersons(command : JsValue) : Map[String, Person] = {

    val personsJs = (command \ "persons").asOpt[JsObject] match {
      case Some(j) => j.fieldSet
        .filter( p => Player.PersonSlots.contains(p._1) )
        .filter( p => p._2.asOpt[JsObject].getOrElse(Json.obj()).fieldSet.count( w => Person.WeaponSlots.contains(w._1) ) >= 2 )
      case None => Set.empty
    }

    if (personsJs.size >= 3) {

      val persons = personsJs.map { js =>
        val p = Person.findBy( (js._2 \ "id").asOpt[Long].getOrElse(0))
        if (p.nonEmpty) {
          js._1 -> p.head
        } else js._1 -> null
      }.filter( p => p._2 != null ).toMap

      if (persons.size >= 3) {

        val weapons = personsJs.map { js =>
          val w1 = Weapon.findBy( (js._2 \ Person.WeaponSlot1).asOpt[Long].getOrElse(0))
          val w2 = Weapon.findBy( (js._2 \ Person.WeaponSlot2).asOpt[Long].getOrElse(0))
          if (w1.nonEmpty && w2.nonEmpty) {
            js._1 -> Map[String, Weapon](Person.WeaponSlot1 -> w1.head, Person.WeaponSlot2 -> w2.head)
          } else js._1 -> Map.empty[String, Weapon]
        }.toMap

        if (weapons.count( w => w._2.size >= 2 ) >= 3) {
          return persons.map { p => p._2.weapons = weapons(p._1); p }
        }
      }
    }

    Map.empty
  }
}
