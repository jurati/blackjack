package blackjack.entity.card

import cats.effect.IO
import cats.effect.concurrent.Ref

import scala.util.Random

trait Deck[F[_]] {
  def draw(count: Int): F[List[Card]]

  def drawOne: F[Card]

  def get: F[List[Card]]
}

object Deck {
  def createDeck: List[Card] = {
    val allCards = for {
      rank <- Rank.allRanks
      suit <- Suit.allSuits
    } yield Card(rank, suit)

    Random.shuffle(List.fill(1)(allCards).flatten)
  }

  def create: IO[Deck[IO]] = Ref.of[IO, List[Card]](createDeck).map { ref =>
    new Deck[IO] {
      override def draw(count: Int): IO[List[Card]] = for {
        cards <- ref.get
        _ <- ref.update(_.drop(count))
      } yield cards.take(count)

      override def get: IO[List[Card]] = ref.get

      override def drawOne: IO[Card] = for {
        cards <- draw(1)
      } yield cards.head
    }
  }
}