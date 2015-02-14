package session

import models.User
import com.redis.RedisClient
import play.api.Play

trait SessionRepository {

  def sessionManager: Session

  trait Session {
    val redisClient : RedisClient
    def redis: RedisClient
    def uuid: String
    def authorize(user: User): String
    def authorized(uuid: String): Option[String]
  }
}

trait SessionRepositoryComponentImpl extends SessionRepository {

  def sessionManager: Session = new SessionRepositoryImpl()

  class SessionRepositoryImpl extends Session {

    val redisClient = new RedisClient(
      Play.current.configuration.getString("redis.default.host").get,
      Play.current.configuration.getInt("redis.default.port").get)

    def redis = {
      redisClient
    }

    def uuid : String = {
      java.util.UUID.randomUUID.toString
    }

    def authorize(user: User): String = {
      val id = uuid
      redisClient.set(id, "" + user.id.get + "|" + System.currentTimeMillis())
      id
    }

    def authorized(uuid: String): Option[String] = {
      redisClient.get(uuid)
    }
  }
}
