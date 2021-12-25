package blackjack

import blackjack.entity.card.{Card, Rank, Suit}
import blackjack.entity.player.{Hand, Player, Wait}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class PlayerSpec extends AnyFreeSpec with Matchers {
  val handK2: Hand = Hand(List(Card(Rank.King, Suit.Spades), Card(Rank.Two, Suit.Hearts)))
  val hand6A: Hand = Hand(List(Card(Rank.Six, Suit.Spades), Card(Rank.Ace, Suit.Hearts)))
  val player: Player = Player(Map(0 -> handK2), Wait, 500, 0)

  "Player hand is added correctly" - {
    player.addHand(hand6A) shouldBe Player(Map(0 -> handK2, 1 ->hand6A), Wait, 500, 0)
  }

  "Player bet is adjusted correctly" - {
    player.adjustBet(100) shouldBe Player(Map(0 -> handK2), Wait, 400, 100)
  }
}
