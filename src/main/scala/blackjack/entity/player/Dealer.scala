package blackjack.entity.player

final case class Dealer(hand: Hand) {
  def canDraw: Boolean = hand.score < 17
}
