package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json.Json

case class User(id: Option[Long] = None, nickname: String, password: String = null) {

  def toJson = {
    Json.obj(
      "id" -> id.get,
      "nickname" -> nickname)
  }
}

object User {

  private val parser : RowParser[User] = {
    get[Option[Long]]("id") ~
    get[String]("nickname") ~
    get[String]("password") map {
      case id ~ nickname ~ password => User(id, nickname, password)
    }
  }

  def save(user: User) = {
    val id = DB.withConnection { implicit connection =>
      SQL("""
    		  INSERT INTO user (nickname, password)
    		  VALUES({nickname}, {password})
          """).on(
            'nickname -> user.nickname,
            'password -> user.password).executeInsert( scalar[Long] single )
    }
    id
  }

  def list: List[User] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM user").as(parser *)
    }
  }

  def findBy(nickname: String, password: String): List[User] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM user WHERE " +
          "nickname = {nickname} AND " +
          "password = {password}")
          .on('nickname -> nickname, 'password -> password)
          .as(parser *)
    }
  }

  def fromJson(raw: String) : User = {
    val json = Json.parse(raw)
    User(
      (json \ "id").asOpt[Long],
      (json \ "nickname").as[String])
  }
}
