package actors

import akka.actor._
import akka.event.LoggingReceive
import models.{Weapon, Person, Player, User}
import org.joda.time.DateTime
import scala.concurrent.duration._

class GameActor(id: String) extends Actor with ActorLogging {

  val system = ActorSystem("GameActor")

  var players = Set[(ActorRef, Player)]()
  var board : ActorRef = BoardActor()

  val persons = Person.list
  val weapons = Weapon.list

  var stepTimer : Cancellable = null

  def receive = LoggingReceive {

    case subscribe:SubscribeGame =>
      players += (subscribe.actor -> new Player(subscribe.user, Map.empty))
      context watch subscribe.actor

      if (players.size >= 2) {
        players.map { _._1 ! StartGame(id, players.map { _._2 } ) }
      }

    case Options =>
      sender() ! GameOptions(persons, weapons, DateTime.now().getMillis)

      import system.dispatcher

      stepTimer = system.scheduler.schedule(0 milliseconds,
        40 seconds,
        self,
        NothingSelected);

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
case class StartGame(id : String, players : Set[Player])
case class EndGame(id : String)
case class GameOptions(persons: List[Person], weapons: List[Weapon], now: Long)

object Options
object NothingSelected

object Won
object Lose