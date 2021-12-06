package blackjack.entity.player

import blackjack.entity.card.Card
import blackjack.entity.card.Rank

final case class Hand(cards: List[Card]) {
  def score: Int = cards.map(_.rank.value).sum

  def scoreSoft: Int = cards.map(card => if (card.rank == Rank.Ace) 1 else card.rank.value).sum

  def isBust: Boolean = score > 21

  def addCard(card: Card): Hand = Hand(cards :+ card)

  override def toString: String = cards.mkString(", ")
}
