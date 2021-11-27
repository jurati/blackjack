package blackjack.entity

import scala.util.Random

class Deck {
  val cards: List[Card] = {
    val allCards = for {
      rank <- Rank.allRanks
      suit <- Suit.allSuits
    } yield Card(rank, suit)

    Random.shuffle(List.fill(count)(allCards).flatten)
  }

  def count = 6
}

object Test extends App {
  val deck = new Deck
}