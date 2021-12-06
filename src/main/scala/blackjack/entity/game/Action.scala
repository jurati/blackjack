package blackjack.entity.game

sealed trait Action

object Action {
  case object Hit extends Action
  case object Stand extends Action
}