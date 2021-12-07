package blackjack.server

import blackjack.entity.player.{Hand, Player, Wait}
import blackjack.server.WebServer.GameState

import java.util.UUID

case class Session(id: UUID) {
  def connect(state: GameState): GameState = {
    val (dealer, players) = state

    (dealer, players + (id -> Player(Hand(List.empty), Wait, 500, 0)))
  }

  def disconnect(state: GameState): GameState = {
    val (dealer, players) = state

    (dealer, players - id)
  }
}
