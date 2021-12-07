package blackjack.entity.game

sealed trait Action

object Action {
  case object Hit extends Action
  case object Stand extends Action
  case object DoubleDown extends Action
  case object Surrender extends Action
}