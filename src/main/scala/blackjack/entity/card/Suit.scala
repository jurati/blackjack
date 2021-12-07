package blackjack.entity.card

sealed trait Suit {
  def name: String

  override def toString: String = name
}

object Suit {
  def unsafeOf(suit: String): Suit = suit match {
    case "hearts" => Hearts
    case "diamonds" => Diamonds
    case "spades" => Spades
    case "clubs" => Clubs
  }

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