package blackjack.entity.player

sealed trait Status
sealed trait Finish extends Status

case object Wait extends Status
case object Play extends Status
case object Bust extends Finish
case object Stand extends Finish

final case class Player(hand: Hand, status: Status) {
  def canHit: Boolean = {
    hand.scoreSoft < 21
  }

  def isFinished: Boolean = status.isInstanceOf[Finish]
}
