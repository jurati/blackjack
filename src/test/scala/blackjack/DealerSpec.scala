package blackjack

import blackjack.entity.card.{Card, Rank, Suit}
import blackjack.entity.player.{Dealer, Hand}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class DealerSpec extends AnyFreeSpec with Matchers {
  "Dealer canDraw if hand score < 17" - {
    Dealer(Hand(List(Card(Rank.King, Suit.Spades), Card(Rank.Two, Suit.Hearts)))).canDraw shouldBe true
    Dealer(Hand(List(Card(Rank.Six, Suit.Spades), Card(Rank.Ace, Suit.Hearts)))).canDraw shouldBe false
  }
}
