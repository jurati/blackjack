package blackjack.entity.player

import blackjack.entity.card.Card
import blackjack.entity.card.Rank.Ace

import scala.annotation.tailrec

sealed trait HandStatus
sealed trait Finish extends HandStatus
sealed trait Play extends HandStatus

case object Waiting extends Play
case object Current extends Play

case object Bust extends Finish
case object Stand extends Finish
case object DoubleDown extends Finish
case object Surrender extends Finish

final case class Hand(cards: List[Card], status: HandStatus = Waiting) {
  val score: Int = getScoreWithAces(cards.map(_.rank.value).sum, cards.count(_.rank == Ace))

  val isBust: Boolean = score > 21

  val isBlackjack: Boolean = cards.size == 2 && score == 21

  val canHit: Boolean = score < 21

  val isFinished: Boolean = status.isInstanceOf[Finish]

  val isCurrent: Boolean = status == Current

  val isWaiting: Boolean = status == Waiting

  val isSurrender: Boolean = status == Surrender

  val canSplit: Boolean = cards.size == 2 && cards.map(_.rank.value).distinct.length == 1

  def getPayoutRatio(dealerHand: Hand): Float = {
    if (isSurrender) 0.5.floatValue()
    else if (isBust || dealerHand.isBlackjack || (!dealerHand.isBust && dealerHand.score > score)) 0
    else if (isBlackjack) 2.5.floatValue()
    else if (dealerHand.score == score) 1
    else  2
  }

  def addCard(card: Card): Hand = copy(cards = cards :+ card)

  override def toString: String = cards.mkString(", ")

  @tailrec
  private def getScoreWithAces(currentScore: Int, highAceCount: Int): Int =
    if (currentScore > 21 && highAceCount != 0) getScoreWithAces(currentScore - 10, highAceCount - 1)
    else currentScore
}
