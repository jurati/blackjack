package blackjack.entity

sealed trait Rank {
  def value: Int
  def symbol: Char

  override def toString: String = symbol.toString
}

object Rank {
  case object Two extends Rank {
    override def value: Int = 2
    override def symbol: Char = '2'
  }

  case object Three extends Rank {
    override def value: Int = 3
    override def symbol: Char = '3'
  }

  case object Four extends Rank {
    override def value: Int = 4
    override def symbol: Char = '4'
  }

  case object Five extends Rank {
    override def value: Int = 5
    override def symbol: Char = '5'
  }

  case object Six extends Rank {
    override def value: Int = 6
    override def symbol: Char = '6'
  }

  case object Seven extends Rank {
    override def value: Int = 7
    override def symbol: Char = '7'
  }

  case object Eight extends Rank {
    override def value: Int = 8
    override def symbol: Char = '8'
  }

  case object Nine extends Rank {
    override def value: Int = 9
    override def symbol: Char = '9'
  }

  case object Ten extends Rank {
    override def value: Int = 10
    override def symbol: Char = 'T'
  }

  case object Jack extends Rank {
    override def value: Int = 10
    override def symbol: Char = 'J'
  }

  case object Queen extends Rank {
    override def value: Int = 10
    override def symbol: Char = 'Q'
  }

  case object King extends Rank {
    override def value: Int = 10
    override def symbol: Char = 'K'
  }

  case object Ace extends Rank {
    override def value: Int = 10
    override def symbol: Char = 'A'
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