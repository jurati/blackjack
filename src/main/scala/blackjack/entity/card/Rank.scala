package blackjack.entity.card

trait Rank {
  def value: Int
  def symbol: String

  override def toString: String = symbol
}

object Rank {
  def of(rank: String): Rank = rank match {
    case "2" => Two
    case "3" => Three
    case "4" => Four
    case "5" => Five
    case "6" => Six
    case "7" => Seven
    case "8" => Eight
    case "9" => Nine
    case "10" => Ten
    case "J" => Jack
    case "Q" => Queen
    case "K" => King
    case "A" => Ace
  }

  case object Two extends Rank {
    override def value: Int = 2
    override def symbol: String = "2"
  }

  case object Three extends Rank {
    override def value: Int = 3
    override def symbol: String = "3"
  }

  case object Four extends Rank {
    override def value: Int = 4
    override def symbol: String = "4"
  }

  case object Five extends Rank {
    override def value: Int = 5
    override def symbol: String = "5"
  }

  case object Six extends Rank {
    override def value: Int = 6
    override def symbol: String = "6"
  }

  case object Seven extends Rank {
    override def value: Int = 7
    override def symbol: String = "7"
  }

  case object Eight extends Rank {
    override def value: Int = 8
    override def symbol: String = "8"
  }

  case object Nine extends Rank {
    override def value: Int = 9
    override def symbol: String = "9"
  }

  case object Ten extends Rank {
    override def value: Int = 10
    override def symbol: String = "T"
  }

  case object Jack extends Rank {
    override def value: Int = 10
    override def symbol: String = "J"
  }

  case object Queen extends Rank {
    override def value: Int = 10
    override def symbol: String = "Q"
  }

  case object King extends Rank {
    override def value: Int = 10
    override def symbol: String = "K"
  }

  case object Ace extends Rank {
    override def value: Int = 10
    override def symbol: String = "A"
  }

  def allRanks: List[Rank] =
    List(
      Two,
      Three,
      Four,
      Five,
      Six,
      Seven,
      Eight,
      Nine,
      Ten,
      Jack,
      Queen,
      King,
      Ace,
    )
}