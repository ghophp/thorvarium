package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.actor.ActorRef
import akka.actor.Terminated
import models.User
import play.api.libs.json.{Json, JsValue}
import play.libs.Akka
import akka.actor.Props

class BoardActor extends Actor with ActorLogging {

  var users = Set[(ActorRef, User)]()

  def receive = LoggingReceive {
    case message:Message => users map { _._1 ! message}

    case subscribe:Subscribe =>
      users += (sender -> subscribe.user)
      context watch sender
      users map { _._1 ! members }

    case Terminated(user) =>
      users.find( u => u._1 == user ) match {
        case Some(x) =>
          users -= x
          context unwatch x._1
          users map { _._1 ! members }
        case None => log.error(">>> Terminated actor not found")
      }
  }

  def members : BoardMembers = {
    BoardMembers(Json.toJson(users.map( u => u._2.toJson )))
  }
}

object BoardActor {
  lazy val board = Akka.system().actorOf(Props[BoardActor])
  def apply() = board
}

case class Message(user: User, message: String)
case class BoardMembers(members: JsValue)
case class Subscribe(user: User)