package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.actor.ActorRef
import akka.actor.Terminated
import models.User

class GameActor(id: String) extends Actor with ActorLogging {

  var host:(ActorRef, User) = null
  var guest:(ActorRef, User) = null

  var players = Set[(ActorRef, User)]()

  def receive = LoggingReceive {

    case subscribe:SubscribeGame =>
      players += (subscribe.actor -> subscribe.user)
      context watch subscribe.actor
      subscribe.actor ! StartGame

    case Terminated(user) =>
      players.find( u => u._1 == user ) match {
        case Some(x) =>
          players -= x
          context unwatch x._1
        case None => log.error("Terminated actor not found")
      }
  }
}

case class SubscribeGame(actor: ActorRef, user: User)
object StartGame