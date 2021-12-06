package blackjack.entity.game

import blackjack.entity.card.Deck
import blackjack.entity.player.{Dealer, Hand, Play, Player, Status, Wait}
import blackjack.server.WebServer.GameState
import cats.effect.IO
import cats.effect.concurrent.Ref

import java.util.UUID

object Game {
  def startGame(refState: Ref[IO, GameState], deck: Deck[IO]): IO[GameState] = {
    val newGameState = for {
      gameState <- refState.get
      (_, players) = gameState
      cards <- deck.draw(1 + players.size * 2)
    } yield {
      var playersCards = cards.tail
      val (firstId, _) = players.head

      val newPlayers = players.map {
        case (id, _) =>
          val playerCards = playersCards.take(2);
          val player = (id, Player(Hand(playerCards), if (id == firstId) Play else Wait))

          playersCards = playersCards.drop(2)

          player
      }

      (Dealer(Hand(List(cards.head))), newPlayers)
    }

    for {
      newGameState <- newGameState
      newGameState <- refState.updateAndGet(_ => newGameState)
    } yield newGameState
  }

  def hit(refState: Ref[IO, GameState], id: UUID, deck: Deck[IO]): IO[GameState] = {
    val newGameState = for {
      gameState <- refState.get
      card <- deck.drawOne
    } yield {
      val (dealer, players) = gameState

      players.get(id) match {
        case Some(player) if player.status == Play =>
          (dealer, players + (id -> Player(Hand(player.hand.cards :+ card), player.status)))
        case _ => gameState
      }
    }

    for {
      newGameState <- newGameState
      newGameState <- refState.updateAndGet(_ => newGameState)
    } yield newGameState
  }

  def finish(refState: Ref[IO, GameState], id: UUID, status: Status): IO[GameState] = {
    val newGameState = for {
      gameState <- refState.get
    } yield {
      val (dealer, players) = gameState

      players.get(id) match {
        case Some(player) => {
          val standingPlayer = id -> Player(player.hand, status)
          val nextPlayer = players.find(player => player._2.status == Wait)

          nextPlayer match {
            case Some((id, player)) => (dealer, players + standingPlayer + (id -> Player(player.hand, Play)))
            case None => (dealer, players + standingPlayer)
          }
        }
        case _ => gameState
      }
    }

    for {
      newGameState <- newGameState
      newGameState <- refState.updateAndGet(_ => newGameState)
    } yield newGameState
  }

  def dealerDraw(refState: Ref[IO, GameState], deck: Deck[IO]): IO[GameState] = {
    val newGameState = for {
      gameState <- refState.get
      card <- deck.drawOne
    } yield {
      val (dealer, players) = gameState

      (Dealer(Hand(dealer.hand.cards :+ card)), players)
    }

    for {
      newGameState <- newGameState
      newGameState <- refState.updateAndGet(_ => newGameState)
      newGameState <- if (newGameState._1.canDraw) dealerDraw(refState, deck) else IO(newGameState)
    } yield newGameState
  }
}
