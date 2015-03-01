package actors

import akka.actor._
import akka.event.LoggingReceive
import models.{Weapon, Person, Player, User}
import org.joda.time.DateTime
import scala.concurrent.duration._

class GameActor(id: String) extends Actor with ActorLogging {

  val system = ActorSystem("GameActor")

  var players = Map.empty[Player, ActorRef]
  var board : ActorRef = BoardActor()

  var stepTimer : Cancellable = null

  def receive = LoggingReceive {

    case subscribe:SubscribeGame =>

      log.info("== Player enter :: "+ subscribe.user.nickname +" ==")

      players += (new Player(subscribe.user, Map.empty) -> subscribe.actor)
      context watch subscribe.actor

      if (players.size >= 2) {

        val now = DateTime.now().getMillis
        log.info("== Start choose timer at :: "+ now +" ==")

        players.map { _._2 ! StartGame(id,
          players.map(_._1).toSet,
          Person.list,
          Weapon.list,
          now) }

        chooseTimer()
      }

    case set:PlayerSet =>

      players.find( _._1.user.id.get == set.user ) match {
        case Some(p) =>
          p._1.persons = set.persons
          if (players.count( _._1.persons.size >= 3 ) >= 2) {

            stepTimer.cancel()

            val now = DateTime.now().getMillis
            log.info("== Game ready at :: "+ now +" ==")

            players.map { _._2 ! GameReady(players.map(_._1).toSet, now) }
          }
        case None => log.info("== PlayerSet not find :: "+ set.user +" ==")
      }

    case NothingSelected if sender == self =>
      players.map { _._2 ! NothingSelected }
      endGame()

    case Terminated(user) =>
      players.find( u => u._2 == user ) match {
        case Some(x) =>
          players -= x._1
          context unwatch x._2

          players.map { _._2 ! Won }
          endGame()

        case None => log.error("== Terminated actor not found ==")
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

    log.info("== Game has ended :: "+ id +" ==")

    board ! EndGame(id)
    context stop self
  }

  def getId = {
    id
  }
}

case class SubscribeGame(user: User, actor: ActorRef)
case class EndGame(id : String)
case class StartGame(id : String,
                     players : Set[Player],
                     persons: List[Person],
                     weapons: List[Weapon],
                     now: Long)

case class PlayerSet(user: Long, persons: Map[String, Person])
case class GameReady(players: Set[Player], now: Long)

object NothingSelected
object Won
object Lose