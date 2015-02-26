package actors

import akka.actor._
import akka.event.LoggingReceive
import models.{Weapon, Person, Player, User}
import org.joda.time.DateTime
import scala.concurrent.duration._

class GameActor(id: String) extends Actor with ActorLogging {

  val system = ActorSystem("GameActor")

  var players = Map.empty[ActorRef, Player]
  var board : ActorRef = BoardActor()

  var stepTimer : Cancellable = null

  def receive = LoggingReceive {

    case subscribe:SubscribeGame =>
      players += (subscribe.actor -> new Player(subscribe.user, Map.empty))
      context watch subscribe.actor

      if (players.size >= 2) {

        players.map { _._1 ! StartGame(id,
          players.map(_._2).toSet,
          Person.list,
          Weapon.list,
          DateTime.now().getMillis ) }

        chooseTimer()
      }

    case NothingSelected if sender == self =>
      players.map { _._1 ! NothingSelected }
      endGame()

    case Terminated(user) =>
      players.find( u => u._1 == user ) match {
        case Some(x) =>

          players -= x._1
          context unwatch x._1

          players.map { _._1 ! Won }
          endGame()

        case None => log.error("Terminated actor not found")
      }
  }

  def chooseTimer() = {
    import system.dispatcher

    stepTimer = system.scheduler.scheduleOnce(
      40 seconds,
      self,
      NothingSelected)
  }

  def endGame() = {
    board ! EndGame(id)
    context stop self
  }

  def getId = {
    id
  }
}

case class SubscribeGame(actor: ActorRef, user: User)
case class EndGame(id : String)
case class StartGame(id : String,
                     players : Set[Player],
                     persons: List[Person],
                     weapons: List[Weapon],
                     now: Long)

object NothingSelected

object Won
object Lose