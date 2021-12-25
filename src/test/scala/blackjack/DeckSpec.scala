package blackjack

import blackjack.entity.card.Deck
import cats.effect.IO
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class DeckSpec extends AnyFreeSpec with Matchers {
  "draw draws expected card count" - {
    (for {
      deck <- Deck.create
      cards1 <- deck.draw(1)
      cards2 <- deck.draw(2)
      cards3 <- deck.draw(3)
      _ <- IO(cards1.length shouldBe 1)
      _ <- IO(cards2.length shouldBe 2)
      _ <- IO(cards3.length shouldBe 3)
    } yield ()).unsafeRunSync()
  }
}
