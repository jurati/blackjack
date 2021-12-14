package blackjack.server

import blackjack.entity.game.Game
import blackjack.entity.player.{Hand, Player, Wait}
import blackjack.server.WebServer.{MessageQueues, MessageQueue}
import cats.effect.IO
import cats.effect.concurrent.Ref

import java.util.UUID

case class Session(id: UUID) {
  def connect(state: Game, queues: MessageQueues, queue: MessageQueue): (Game, MessageQueues) =
    if (!connected(state)) (state.copy(players = state.players + (id -> Player(Hand(List.empty), Wait, 500, 0))), queues + (id -> queue))
    else (state, queues)

  def disconnectFromGame(state: Game): Game = state.copy(players = state.players - id)

  def disconnectFromQueue(queues: MessageQueues): MessageQueues = queues - id

  def connected(state: Game): Boolean = state.players.exists(_._1 == id)
}
