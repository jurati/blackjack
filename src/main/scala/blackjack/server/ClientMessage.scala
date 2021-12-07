package blackjack.server

import blackjack.entity.game.Action

sealed trait ClientMessage

object ClientMessage {
  final case class GameStatus(status: Int) extends ClientMessage
  final case class Decision(action: Action) extends ClientMessage
  final case class Bet(amount: Float) extends ClientMessage
}
