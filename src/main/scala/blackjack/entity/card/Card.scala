package blackjack.entity.card

import scala.util.Random

final case class Card(rank: Rank, suit: Suit)

object Card {
  def getCardsForDeck: List[Card] = {
    val allCards = for {
      rank <- Rank.allRanks
      suit <- Suit.allSuits
    } yield Card(rank, suit)

    Random.shuffle(List.fill(1)(allCards).flatten)
  }
}