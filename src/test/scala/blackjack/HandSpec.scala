package blackjack

import blackjack.entity.card.{Card, Rank, Suit}
import blackjack.entity.player.{Hand, Surrender}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class HandSpec extends AnyFreeSpec with Matchers {
  val handK2: Hand = Hand(List(Card(Rank.King, Suit.Spades), Card(Rank.Two, Suit.Hearts)))
  val hand6A: Hand = Hand(List(Card(Rank.Six, Suit.Spades), Card(Rank.Ace, Suit.Hearts)))
  val handKA: Hand = Hand(List(Card(Rank.King, Suit.Spades), Card(Rank.Ace, Suit.Hearts)))
  val handAA: Hand = Hand(List(Card(Rank.Ace, Suit.Spades), Card(Rank.Ace, Suit.Hearts)))
  val handAAQ: Hand = Hand(List(Card(Rank.Ace, Suit.Spades), Card(Rank.Ace, Suit.Hearts), Card(Rank.Queen, Suit.Spades)))
  val handAQT: Hand = Hand(List(Card(Rank.Ace, Suit.Spades), Card(Rank.Queen, Suit.Hearts), Card(Rank.Ten, Suit.Spades)))
  val hand999: Hand = Hand(List(Card(Rank.Nine, Suit.Spades), Card(Rank.Nine, Suit.Hearts), Card(Rank.Nine, Suit.Spades)))

  "Hand score is calculated correctly" - {
    handK2.score shouldBe 12
    hand6A.score shouldBe 17
    handKA.score shouldBe 21
    handAA.score shouldBe 12
    handAAQ.score shouldBe 12
    handAQT.score shouldBe 21
    hand999.score shouldBe 27
  }

  "Hand is bust if score < 21" - {
    handK2.isBust shouldBe false
    handKA.isBust shouldBe false
    hand999.isBust shouldBe true
  }

  "Hand is blackjack 2 cards score = 21" - {
    handK2.isBlackjack shouldBe false
    handKA.isBlackjack shouldBe true
    handAQT.isBlackjack shouldBe false
    hand999.isBlackjack shouldBe false
  }

  "Hand can git if score < 21" - {
    handK2.canHit shouldBe true
    handKA.canHit shouldBe false
    handAQT.canHit shouldBe false
    hand999.canHit shouldBe false
  }

  "Hand can split if has 2 cards with same score" - {
    handK2.canSplit shouldBe false
    handAA.canSplit shouldBe true
    hand999.canSplit shouldBe false
  }

  "Hand payout ratio is calculated correctly" - {
    handK2.copy(status = Surrender).getPayoutRatio(handKA) shouldBe 0.5
    hand999.getPayoutRatio(hand999) shouldBe 0
    handKA.getPayoutRatio(handKA) shouldBe 0
    hand6A.getPayoutRatio(handAQT) shouldBe 0
    handKA.getPayoutRatio(hand999) shouldBe 2.5
    handAQT.getPayoutRatio(handAQT) shouldBe 1
    handK2.getPayoutRatio(hand999) shouldBe 2
  }
}
