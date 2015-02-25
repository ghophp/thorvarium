package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.actor.ActorRef
import akka.actor.Terminated
import models.User

class GameActor(id: String) extends Actor with ActorLogging {

  var players = Set[(ActorRef, User)]()
  var board : ActorRef = BoardActor()

  def receive = LoggingReceive {

    case subscribe:SubscribeGame =>
      players += (subscribe.actor -> subscribe.user)
      context watch subscribe.actor

      if (players.size >= 2) {
        players.map { _._1 ! StartGame(id, players.map { _._2 } ) }
      }

    case Terminated(user) =>
      players.find( u => u._1 == user ) match {
        case Some(x) =>

          players -= x
          context unwatch x._1

          players.map { _._1 ! Won }
          board ! EndGame(id)

          context stop self

        case None => log.error("Terminated actor not found")
      }
  }
}

case class SubscribeGame(actor: ActorRef, user: User)
case class StartGame(id : String, players : Set[User])
case class EndGame(id : String)

object Won
object Lose