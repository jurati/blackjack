package blackjack.entity.player

sealed trait Status
sealed trait Finish extends Status
sealed trait Play extends Status

case object Wait extends Play
case object Turn extends Play
case object BetPlaced extends Play

case object Bust extends Finish
case object Stand extends Finish
case object DoubleDown extends Finish
case object Surrender extends Finish

final case class Player(hand: Hand, status: Status, balance: Float, bet: Float) {
  val canHit: Boolean = hand.score < 21

  val canDoubleDown: Boolean = hand.cards.size == 2

  val isFinished: Boolean = status.isInstanceOf[Finish]

  val isPlaying: Boolean = status.isInstanceOf[Play]

  val isTurn: Boolean = status == Turn

  val isWaiting: Boolean = status == Wait

  val isBetPlaced: Boolean = status == BetPlaced
}
