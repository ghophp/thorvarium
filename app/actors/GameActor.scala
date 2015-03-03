package actors

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.LoggingReceive
import game.GameLoop
import game.models.{GamingSet, GamingPlayer}
import models.{Weapon, Person, Player, User}
import org.joda.time.DateTime
import scala.concurrent.duration._

class GameActor(id: String) extends Actor with ActorLogging {

  val system = ActorSystem("GameActor")

  var players = Map.empty[Player, ActorRef]
  var board : ActorRef = BoardActor()

  var stepTimer : Cancellable = null
  var gameLoop : GameLoop = null

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

        timer(NothingSelected)
      }

    case set:PlayerSet =>

      players.find( _._1.user.id.get == set.user ) match {
        case Some(p) =>
          p._1.persons = set.persons
          if (players.count( _._1.persons.size >= 3 ) >= 2 && gameLoop == null) {

            stepTimer.cancel()

            val now = DateTime.now().getMillis
            log.info("== Game ready at :: "+ now +" ==")

            val playerList = players.map(_._1).toList
            players.map { _._2 ! GameReady(playerList.toSet, now) }

            gameLoop = new GameLoop(
              new GamingPlayer(playerList(0), GamingPlayer.Player1),
              new GamingPlayer(playerList(1), GamingPlayer.Player2))

            timer(TurnEnd)
          }
        case None => log.info("== PlayerSet not find :: "+ set.user +" ==")
      }

    case set:PlayerTurnSet =>
      if (gameLoop != null && set.input != null) {

        players.find( _._1.user.id.get == set.user ) match {
          case Some(p) =>
            if (p._1.user.id.get == gameLoop.player1.player.user.id.get) {
              gameLoop.player1.input = set.input
            } else if (p._1.user.id.get == gameLoop.player2.player.user.id.get) {
              gameLoop.player2.input = set.input
            }
          case None => log.info("== PlayerTurnSet not find :: "+ set.user +" ==")
        }
      }

    case TurnEnd if sender == self =>
      if (gameLoop != null && stepTimer != null) {

        stepTimer.cancel()
        gameLoop.state = GameLoop.Running
        var lastTime = System.currentTimeMillis()

        while (gameLoop.state == GameLoop.Running) {
          val current = System.currentTimeMillis()

          gameLoop.reset()
          gameLoop.update(current - lastTime)

          lastTime = current
        }

        gameLoop.turns += 1
        timer(TurnEnd)
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

  def timer(message: Any) = {
    import system.dispatcher

    stepTimer = system.scheduler.scheduleOnce(
      Duration.create(40, TimeUnit.SECONDS),
      self,
      message)
  }

  def endGame() = {

    log.info("== Game has ended :: "+ id +" ==")

    if (stepTimer != null) {
      stepTimer.cancel()
    }

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
case class PlayerTurnSet(user: Long, input: GamingSet)

object TurnEnd
object NothingSelected
object Won
object Lose