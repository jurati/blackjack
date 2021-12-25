package blackjack

import blackjack.entity.card.{Card, Deck, Rank, Suit}
import blackjack.entity.game.Game
import blackjack.entity.player.{BetPlaced, Current, Dealer, DoubleDown, Finished, Hand, Player, Stand, Turn, Wait}
import cats.effect.IO
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID
import java.util.UUID.randomUUID

class GameSpec extends AnyFreeSpec with Matchers {
  val uuid1: UUID = randomUUID
  val uuid2: UUID = randomUUID
  val dealer: Dealer = Dealer(Hand(List.empty))
  val player1: (UUID, Player) = uuid1 -> Player(Map.empty, BetPlaced, 490, 10)
  val player2: (UUID, Player) = uuid2 -> Player(Map.empty, BetPlaced, 490, 10)

  "Game starts correctly" - {
    (for {
      deck <- Deck.create
      game = Game(dealer, Map(player1, player2))
      newGame <- game.start(deck, Map.empty)
    } yield {
      newGame.players.map(_._2.hands.map(_._2.cards.length shouldBe 2))
      newGame.dealer.hand.cards.length shouldBe 1
      newGame.players(uuid1).status shouldBe Turn
      newGame.players(uuid2).status shouldBe Wait
    }).unsafeRunSync()
  }

  "Player can hit" - {
    val game: Game = Game(
      dealer,
      Map(
        uuid1 -> Player(Map(0 -> Hand(List(Card(Rank.Three, Suit.Spades), Card(Rank.Two, Suit.Hearts)), Current)), Turn, 490, 10),
        uuid2 -> Player(Map(0 -> Hand(List(Card(Rank.Four, Suit.Spades), Card(Rank.Ten, Suit.Hearts)))), Wait, 490, 10)
      )
    )

    (for {
      deck <- Deck.create
      newGame <- game.hit(uuid2, deck, Map.empty)
      _ <- IO(newGame.players(uuid2).hands(0).cards.length shouldBe 2)
      newGame <- newGame.hit(uuid1, deck, Map.empty)
    } yield {
      newGame.players(uuid1).hands(0).cards.length shouldBe  3
    }).unsafeRunSync()
  }

  "Players status changes on stand" - {
    val game: Game = Game(dealer, Map(player1, player2))

    (for {
      deck <- Deck.create
      newGame <- game.start(deck, Map.empty)
      newGame <- newGame.finish(uuid1, Stand, Map.empty)
      newGame <- newGame.turn(deck, Map.empty)
    } yield {
      newGame.players(uuid1).status shouldBe Finished
      newGame.players(uuid2).status shouldBe Turn
    }).unsafeRunSync()
  }

  "Player can bet" - {
    val game: Game = Game(dealer, Map(uuid1 -> Player(Map.empty, Wait, 500, 0)))

    (for {
      newGame <- game.bet(uuid1, 10)
    } yield {
      newGame.players(uuid1).status shouldBe BetPlaced
      newGame.players(uuid1).bet shouldBe 10
      newGame.players(uuid1).balance shouldBe 490
    }).unsafeRunSync()
  }

  "Player can double down" - {
    (for {
      deck <- Deck.create
      newGame <- Game(dealer, Map(uuid1 -> Player(Map(0 -> Hand(List(Card(Rank.Three, Suit.Spades), Card(Rank.Two, Suit.Hearts)), Current)), Turn, 490, 10))).doubleDown(uuid1, deck, Map.empty)
    } yield {
      newGame.players(uuid1).status shouldBe Finished
      newGame.players(uuid1).hands(0).status shouldBe DoubleDown
      newGame.players(uuid1).bet shouldBe 20
      newGame.players(uuid1).balance shouldBe 480
    }).unsafeRunSync()
  }

  "Player can split" - {
    (for {
      deck <- Deck.create
      newGame <- Game(dealer, Map(uuid1 -> Player(Map(0 -> Hand(List(Card(Rank.Two, Suit.Spades), Card(Rank.Two, Suit.Hearts)), Current)), Turn, 490, 10))).split(uuid1, deck)
    } yield {
      newGame.players(uuid1).hands.size shouldBe 2
      newGame.players(uuid1).bet shouldBe 20
      newGame.players(uuid1).balance shouldBe 480
    }).unsafeRunSync()
  }
}
