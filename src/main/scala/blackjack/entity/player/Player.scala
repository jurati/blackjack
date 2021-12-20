package blackjack.entity.player

sealed trait Status

case object Wait extends Status
case object Turn extends Status
case object BetPlaced extends Status
case object Finished extends Status


final case class Player(hands: Map[Int, Hand], status: Status, balance: Float, bet: Float) {
  val isTurn: Boolean = status == Turn

  val isWaiting: Boolean = status == Wait

  val isBetPlaced: Boolean = status == BetPlaced

  val isFinished: Boolean = status == Finished

  val isSplitting: Boolean = hands.size > 1

  val currentHand: Option[(Int, Hand)] =  hands.find(_._2.isCurrent)

  val isFirstDecision: Boolean = currentHand match {
    case Some((_, hand)) if isTurn && !isSplitting => hand.cards.length == 2
    case _ => false
  }

  val nextHand: Option[(Int, Hand)] =  hands.find(_._2.isWaiting)

  def addHand(hand: Hand): Player = copy(hands = hands + (hands.size -> hand))

  def adjustBet(amount: Float): Player = copy(bet = bet + amount, balance = balance - amount)
}
