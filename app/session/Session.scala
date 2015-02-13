package session

import models.User
import com.redis.RedisClient
import play.api.Play

trait SessionRepository {

  def sessionManager: Session

  trait Session {
    def redis: RedisClient
    def uuid: String
    def authorize(user: User): String
    def authorized(uuid: String): Option[String]
  }
}

trait SessionRepositoryComponentImpl extends SessionRepository {

  def sessionManager: Session = new SessionRepositoryImpl()

  class SessionRepositoryImpl extends Session {
    def redis = {
      new RedisClient(
        Play.current.configuration.getString("redis.default.host").get,
        Play.current.configuration.getInt("redis.default.port").get)
    }

    def uuid : String = {
      java.util.UUID.randomUUID.toString
    }

    def authorize(user: User): String = {
      uuid.map( id => {
        redis.set(id, "" + user.id + "|" + System.currentTimeMillis()); id
      } )
    }

    def authorized(uuid: String): Option[String] = {
      redis.get(uuid)
    }
  }
}
