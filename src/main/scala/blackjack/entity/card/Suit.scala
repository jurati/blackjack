package blackjack.entity.card

sealed trait Suit {
  val name: String

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
    override val name: String = "hearts"
  }

  case object Diamonds extends Suit {
    override val name: String = "diamonds"
  }

  case object Spades extends Suit {
    override val name: String = "spades"
  }

  case object Clubs extends Suit {
    override val name: String = "clubs"
  }

  def allSuits: List[Suit] = List(Hearts, Diamonds, Clubs, Spades)
}