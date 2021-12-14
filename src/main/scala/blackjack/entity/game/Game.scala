package blackjack.entity.game

import blackjack.entity.card.{Card, Deck}
import blackjack.entity.player.{BetPlaced, Bust, Dealer, DoubleDown, Hand, Player, Status, Surrender, Turn, Wait}
import blackjack.server.{ServerMessage, Session}
import blackjack.server.WebServer.MessageQueues
import cats.effect.{ContextShift, IO}

import java.util.UUID
import scala.concurrent.ExecutionContext.global

final case class Game(dealer: Dealer, players: Map[UUID, Player]) {
  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)

  val finished: Boolean = players.forall(_._2.isFinished)

  val betsOpen: Boolean = players.forall(state => state._2.isBetPlaced || state._2.isWaiting || state._2.isFinished)

  val canStart: Boolean = players.forall(_._2.isBetPlaced)

  def start(deck: Deck[IO], messageQueues: MessageQueues): IO[Game] = {
    if (canStart) for {
      _ <- ServerMessage.sendMessage("Game started. Good Luck!", messageQueues)
      cards <- deck.draw(1 + players.size * 2)
      (firstId, _) = players.head
    } yield {
      var playersCards = cards.tail

      val newPlayers = players.map {
        case (id, player) =>
          val playerCards = playersCards.take(2)
          val updatedPlayer = (id, player.copy(hand = Hand(playerCards), status = if (id == firstId) Turn else Wait))

          playersCards = playersCards.drop(2)
          updatedPlayer
      }

      Game(Dealer(Hand(List(cards.head))), newPlayers)
    }
    else IO(this)
  }

  def hit(session: Session, deck: Deck[IO], messageQueues: MessageQueues): IO[Game] = {
    players.get(session.id) match {
      case Some(player) if player.isTurn =>
        if (player.canHit) for {
          card <- deck.drawOne
          hand = player.hand.addCard(card)
          gameAfterHit = copy(players = players + (session.id -> player.copy(hand = hand)))
          updatedGame <- if (hand.isBust) gameAfterHit.finish(session.id, Bust, messageQueues) else IO(gameAfterHit)
        } yield updatedGame
        else finish(session.id, Bust, messageQueues)
      case _ => IO(this)
    }
  }

  def finish(id: UUID, status: Status, messageQueues: MessageQueues): IO[Game] = {
    players.get(id) match {
      case Some(player) =>
        ServerMessage.sendMessage(s"Score: ${player.hand.score}. Status: ${status}" , messageQueues, Some(id)) *> IO {
          if (player.isTurn) {
            val finishedPlayer = id -> player.copy(status = status)

            players.find(player => player._2.isWaiting) match {
              case Some((id, player)) => copy(players = players + finishedPlayer + (id -> player.copy(status = Turn)))
              case _ => copy(players = players + finishedPlayer)
            }
          } else {
            this
          }
        }
      case _ => IO(this)
    }
  }

  def bet(id: UUID, amount: Float): IO[Game] = IO {
    players.get(id) match {
      case Some(player) if (this.betsOpen && !player.isBetPlaced) => copy(players = players + (id -> player.copy(status = BetPlaced, balance = player.balance - amount, bet = amount)))
      case _ => this
    }
  }

  def doubleDown(id: UUID, deck: Deck[IO], messageQueues: MessageQueues): IO[Game] = {
    players.get(id) match {
      case Some(player) if player.isTurn && player.canDoubleDown => for {
        card <- deck.drawOne
        hand = player.hand.addCard(card)
        updatedPlayer = player.copy(hand = hand, balance = player.balance - player.bet, bet = player.bet * 2)
        game <- copy(players = players + (id -> updatedPlayer)).finish(id, if (hand.isBust) Bust else DoubleDown, messageQueues)
      } yield game
      case _ => IO(this)
    }
  }

  def surrender(id: UUID, messageQueues: MessageQueues): IO[Game] = {
    players.get(id) match {
      case Some(player) if player.isTurn => copy(players = players + (id -> player.copy(balance = player.balance + player.bet / 2))).finish(id, Surrender, messageQueues)
      case _ => IO(this)
    }
  }

  def dealerDraw(card: Card): Game = copy(dealer = dealer.copy(hand = dealer.hand.addCard(card)))

  def turn(deck: Deck[IO], messageQueues: MessageQueues): IO[Game] = {
    if (finished)
      if (dealer.canDraw) for {
        card <- deck.drawOne
        game <- dealerDraw(card).turn(deck, messageQueues)
      } yield game
      else processResult
    else
      players.find(_._2.isTurn) match {
        case Some((id, player)) => ServerMessage.sendMessage(s"You have ${player.hand.score}. What will you do?" , messageQueues, Some(id)) *> IO(this)
        case None => IO(this)
      }
  }

  def processResult: IO[Game] = {
    val updatedPlayers = players.map {
      case (id, player) =>
        if (player.status != Surrender) (id, Player(player.hand, Wait, player.balance + player.bet * player.hand.getPayoutRatio(dealer.hand), 0))
        else (id, player)
    }

    IO(copy(players = updatedPlayers))
  }
}
