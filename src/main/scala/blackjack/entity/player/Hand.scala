package blackjack.entity.player

import blackjack.entity.card.Card

final case class Hand(cards: List[Card]) {
  def score: Int = cards.foldLeft(0)((acc, a) => acc + a.rank.value)

  def isBust: Boolean = score > 21

  def addCard(card: Card): Hand = Hand(cards :+ card)
}
