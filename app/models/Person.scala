package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json.Json

case class Person(id: Option[Long] = None,
                  name: String,
                  life: Int,
                  speed: Int,
                  size: Int,
                  distance: Int,
                  weapons : Map[String, Weapon]) {

  def toJson = {
    Json.obj(
      "id" -> id.get,
      "name" -> name,
      "life" -> life,
      "speed" -> speed,
      "size" -> size,
      "distance" -> distance)
  }
}

object Person {

  private val parser : RowParser[Person] = {
    get[Option[Long]]("id") ~
        get[String]("name") ~
        get[Int]("life") ~
        get[Int]("speed") ~
        get[Int]("size") ~
        get[Int]("distance") map {
      case id ~ name ~ life ~ speed ~ size ~ distance =>
        Person(id, name, life, speed, size, distance, Map.empty)
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
}
