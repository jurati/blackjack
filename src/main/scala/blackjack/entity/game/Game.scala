package blackjack.entity.game

import blackjack.entity.card.Card
import blackjack.entity.player.{BetPlaced, Bust, Dealer, DoubleDown, Hand, Status, Surrender, Turn, Wait}
import blackjack.server.WebServer.GameState

import java.util.UUID

object Game {
  def startGame(gameState: GameState, cards: List[Card]): GameState = {
    val (_, players) = gameState
    var playersCards = cards.tail
    val (firstId, _) = players.head

    val newPlayers = players.map {
      case (id, player) =>
        val playerCards = playersCards.take(2);
        val updatedPlayer = (id, player.copy(hand = Hand(playerCards), status = if (id == firstId) Turn else Wait))

        playersCards = playersCards.drop(2)
        updatedPlayer
    }

    (Dealer(Hand(List(cards.head))), newPlayers)
  }

  def hit(gameState: GameState, id: UUID, card: Card): GameState = {
    val (dealer, players) = gameState

    players.get(id) match {
      case Some(player) if player.status == Turn =>
        val hand = Hand(player.hand.cards :+ card)
        val updatedPlayers = players + (id -> player.copy(hand = hand))

        if (hand.isBust) finish((dealer, updatedPlayers), id, Bust) else (dealer, updatedPlayers)
      case _ => gameState
    }
  }

  def finish(gameState: GameState, id: UUID, status: Status): GameState = {
    val (dealer, players) = gameState

    players.get(id) match {
      case Some(player) =>
        if (player.status == Turn) {
          val standingPlayer = id -> player.copy(status = status)

          players.find(player => player._2.status == Wait) match {
            case Some((id, player)) => (dealer, players + standingPlayer + (id -> player.copy(status = Turn)))
            case None => (dealer, players + standingPlayer)
          }
        } else gameState
      case _ => gameState
    }
  }

  def bet(gameState: GameState, id: UUID, amount: Float): GameState = {
    val (dealer, players) = gameState

    players.get(id) match {
      case Some(player) =>
        if (betsOpen(gameState) && player.status != BetPlaced) (dealer, players + (id -> player.copy(status = BetPlaced, balance = player.balance - amount, bet = amount)))
        else gameState
      case _ => gameState
    }
  }

  def doubleDown(gameState: GameState, id: UUID, card: Card): GameState = {
    val (dealer, players) = gameState

    players.get(id) match {
      case Some(player) if player.status == Turn =>
        val hand = Hand(player.hand.cards :+ card)
        val updatedPlayer = player.copy(hand = hand, balance = player.balance - player.bet, bet = player.bet * 2)

        finish((dealer, players + (id -> updatedPlayer)), id, if (hand.isBust) Bust else DoubleDown)
      case _ => gameState
    }
  }

  def surrender(gameState: GameState, id: UUID): GameState = {
    val (dealer, players) = gameState

    players.get(id) match {
      case Some(player) if player.status == Turn =>
        finish((dealer, players + (id -> player.copy(balance = player.balance + player.bet / 2))), id, Surrender)
      case _ => gameState
    }
  }

  def dealerDraw(gameState: GameState, card: Card): GameState = {
    val (dealer, players) = gameState

    (Dealer(Hand(dealer.hand.cards :+ card)), players)
  }

  def finished(gameState: GameState): Boolean = gameState._2.forall(_._2.isFinished)

  def betsOpen(gameState: GameState): Boolean =
    gameState._2.forall(state => state._2.status == BetPlaced || state._2.status == Wait || state._2.isFinished)
}
