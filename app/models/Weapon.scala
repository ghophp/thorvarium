package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json.Json

case class Weapon(id: Option[Long] = None,
                  name: String,
                  kind: Int,
                  speed: Int,
                  power: Int,
                  size: Int) {

  val SingleShot = 1
  val TripleShot = 2
  val Barrier = 3

  def toJson = {
    Json.obj(
      "id" -> id.get,
      "name" -> name,
      "kind" -> kind,
      "speed" -> speed,
      "power" -> power,
      "size" -> size)
  }
}

object Weapon {

  private val parser : RowParser[Weapon] = {
    get[Option[Long]]("id") ~
        get[String]("name") ~
        get[Int]("kind") ~
        get[Int]("speed") ~
        get[Int]("power") ~
        get[Int]("size") map {
      case id ~ name ~ kind ~ speed ~ power ~ size =>
        Weapon(id, name, kind, speed, power, size)
    }
  }

  def list: List[Weapon] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM weapon").as(parser *)
    }
  }

  def findBy(id : Long): List[Weapon] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM weapon WHERE id = {id}")
          .on('id -> id)
          .as(parser *)
    }
  }
}
