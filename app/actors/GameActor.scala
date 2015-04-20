package actors

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.LoggingReceive
import game.GameLoop
import game.models.GamingSet
import models.{Weapon, Person, Player, User}
import org.joda.time.DateTime
import scala.collection.mutable
import scala.concurrent.duration._

class GameActor(id: String) extends Actor with ActorLogging {

  val system = ActorSystem("GameActor")

  var players = Map.empty[Player, ActorRef]
  var board : ActorRef = BoardActor()

  var stepTimer : Cancellable = null
  var gameLoop : GameLoop = null
  var readyToTurn : mutable.MutableList[Long] = mutable.MutableList.empty

  def receive = LoggingReceive {

    case subscribe:SubscribeGame =>

      log.info("== Player enter :: "+ subscribe.user.nickname +" ==")

      players += (new Player(subscribe.user, players.size + 1) -> subscribe.actor)
      context watch subscribe.actor

      if (players.size >= 2) {

        val now = DateTime.now().getMillis
        log.info("== Start choose timer at :: "+ now +" ==")

        players.map { _._2 ! StartGame(id,
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

            val playerSet = players.map(_._1).toSet
            gameLoop = new GameLoop(playerSet)
            players.map { _._2 ! GameReady(playerSet, now) }
          }
        case None => log.info("== PlayerSet not found :: "+ set.user +" ==")
      }

    case set:PlayerTurnSet =>
      if (gameLoop != null && set.input != null) {
        players.find( _._1.user.id.get == set.user ) match {
          case Some(p) =>
            gameLoop.players.find( _.user.id.get == p._1.user.id.get ) match {
              case Some(pl) => pl.input = set.input
              case None => log.info("== PlayerTurnSet not found on GameLoop :: "+ set.user +" ==")
            }
          case None => log.info("== PlayerTurnSet not find :: "+ set.user +" ==")
        }
        if (players.count(_._1.input != null) >= 2) {
          self ! TurnEnd
        }
      }

    case ReadyToTurn(user: Long) =>
      readyToTurn += user
      if (readyToTurn.size <= 1) {
        timer(Abandoned)
      } else if (readyToTurn.size >= 2) {

        if (stepTimer != null) {
          stepTimer.cancel()
        }

        players.map { _._2 ! TurnStart }
        timer(TurnEnd)
      }

    case Abandoned if sender == self =>
      readyToTurn.map { u =>
        players.map { p =>
          if (p._1.user.id.get == u) p._2 ! Won else p._2 ! Lose
        }
      }
      endGame()

    case TurnEnd if sender == self =>
      if (gameLoop != null && stepTimer != null) {
        stepTimer.cancel()

        players.map { _._2 ! PreTurn(gameLoop.players.map { p =>
          p.user.id.get.toString -> p.input
        }.toMap) }

        gameLoop.loop()
        if (gameLoop.state == GameLoop.Ended) {

          if (gameLoop.draw()) {
            players.map( _._2 ! Draw )
          } else {
            players.map { p =>
              p._2 ! (if (p._1.user.id.get == gameLoop.winner()) Won else Lose)
            }
          }

          endGame()

        } else {

          readyToTurn.clear()

          players.map { _._2 ! AfterTurn(
            players.map(_._1).toSet,
            gameLoop.turns) }
        }
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
    self ! PoisonPill
  }

  def getId = {
    id
  }
}

case class SubscribeGame(user: User, actor: ActorRef)
case class EndGame(id : String)
case class StartGame(id : String,
                     persons: List[Person],
                     weapons: List[Weapon],
                     now: Long)

case class PlayerSet(user: Long, persons: Map[String, Person])
case class GameReady(players: Set[Player], now: Long)
case class PreTurn(inputs: Map[String, GamingSet])
case class AfterTurn(players: Set[Player], turns: Int)
case class PlayerTurnSet(user: Long, input: GamingSet)
case class ReadyToTurn(user: Long)

object TurnStart
object TurnEnd
object NothingSelected
object Abandoned
object Draw
object Won
object Lose