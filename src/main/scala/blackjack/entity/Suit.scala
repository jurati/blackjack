package blackjack.entity

sealed trait Suit {
  def name: String

  override def toString: String = name
}

object Suit {

  case object Hearts extends Suit {
    override def name: String = "hearts"
  }

  case object Diamonds extends Suit {
    override def name: String = "diamonds"
  }

  case object Spades extends Suit {
    override def name: String = "spades"
  }

  case object Clubs extends Suit {
    override def name: String = "clubs"
  }

  def allSuits: List[Suit] = List(Hearts, Diamonds, Clubs, Spades)
}