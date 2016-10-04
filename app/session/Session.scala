package session

import java.net.URI

import models.User
import com.redis.RedisClient
import play.api.Play

trait SessionRepository {

  def sessionManager: Session

  trait Session {
    val redisUri : URI
    var redisClient : RedisClient
    def redis: RedisClient
    def uuid: String
    def authorize(user: User): String
    def revoke(uuid: String)
    def authorized(uuid: String): Option[User]
  }
}

trait SessionRepositoryComponentImpl extends SessionRepository {

  def sessionManager: Session = new SessionRepositoryImpl()

  class SessionRepositoryImpl extends Session {

    val redisUri = new URI(Play.current.configuration.getString("redis.default.uri").get)
    var redisClient : RedisClient = _

    if (redisUri.getUserInfo != null &&
      !redisUri.getUserInfo.isEmpty) {

      val userInfo = redisUri.getUserInfo.split(":", 2)
      redisClient = new RedisClient(
        redisUri.getHost,
        redisUri.getPort,
        0,
        Some(userInfo{1}))

    } else {
      redisClient = new RedisClient(
        redisUri.getHost,
        redisUri.getPort)
    }

    def redis = {
      redisClient
    }

    def uuid : String = {
      java.util.UUID.randomUUID.toString
    }

    def authorize(user: User): String = {
      val id = uuid
      redisClient.set(id, user.toJson.toString())
      id
    }

    def revoke(uuid: String) = {
      redisClient.del(uuid)
    }

    def authorized(uuid: String): Option[User] = {
      redisClient.get(uuid) match {
        case Some(x:String) => Some(User.fromJson(x))
        case None => None
      }
    }
  }
}
