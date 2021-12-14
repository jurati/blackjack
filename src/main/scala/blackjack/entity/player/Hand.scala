package blackjack.entity.player

import blackjack.entity.card.Card
import blackjack.entity.card.Rank.Ace

import scala.annotation.tailrec

final case class Hand(cards: List[Card]) {
  val score: Int = getScoreWithAces(cards.map(_.rank.value).sum, cards.count(_.rank == Ace))

  val isBust: Boolean = score > 21

  val isBlackjack: Boolean = score == 21

  def getPayoutRatio(dealerHand: Hand): Float = {
    if (isBust || dealerHand.isBlackjack || (!dealerHand.isBust) && dealerHand.score > score) 0
    else if (cards.size == 2 && isBlackjack) 2.5.floatValue()
    else  2
  }

  def addCard(card: Card): Hand = Hand(cards :+ card)

  override def toString: String = cards.mkString(", ")

  @tailrec
  private def getScoreWithAces(currentScore: Int, highAceCount: Int): Int = if (currentScore > 21 && highAceCount != 0) getScoreWithAces(currentScore - 10, highAceCount - 1) else currentScore
}
