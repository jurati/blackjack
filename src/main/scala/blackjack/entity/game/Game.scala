package blackjack.entity.game

import blackjack.entity.card.{Card, Deck}
import blackjack.entity.player.{BetPlaced, Bust, Current, Dealer, DoubleDown, Finished, Hand, HandStatus, Player, Surrender, Turn, Wait, Waiting}
import blackjack.server.{ServerMessage, Session}
import blackjack.server.WebServer.MessageQueues
import cats.effect.{ContextShift, IO}

import java.util.UUID
import scala.concurrent.ExecutionContext.global

final case class Game(dealer: Dealer, players: Map[UUID, Player]) {
  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)

  val finished: Boolean = players.forall(_._2.isFinished)

  val betsOpen: Boolean = players.forall(state => state._2.isBetPlaced || state._2.isWaiting || state._2.isFinished)

  val canStart: Boolean = players.forall(_._2.isBetPlaced)

  def start(deck: Deck[IO], messageQueues: MessageQueues): IO[Game] =
    if (canStart)
      for {
        _ <- ServerMessage.sendMessage("Game started. Good Luck!", messageQueues)
        cards <- deck.draw(1 + players.size * 2)
        (firstId, _) = players.head
      } yield {
        var playersCards = cards.tail

        val newPlayers = players.map {
          case (id, player) =>
            val status = if (id == firstId) Turn else Wait
            val updatedPlayer = (id, player.copy(hands = Map(0 -> Hand(playersCards.take(2), Current)),status = status))

            playersCards = playersCards.drop(2)
            updatedPlayer
        }

        Game(Dealer(Hand(List(cards.head))), newPlayers)
      }
    else
      IO(this)


  def hit(session: Session, deck: Deck[IO], messageQueues: MessageQueues): IO[Game] = {
    players.get(session.id) match {
      case Some(player) if player.isTurn =>
        player.currentHand match {
          case Some((key, hand)) =>
            if (hand.canHit) for {
              card <- deck.drawOne
              currentHand = hand.addCard(card)
              game = copy(players = players + (session.id -> player.copy(hands = player.hands + (key -> currentHand))))
              updatedGame <- if (currentHand.isBust) game.finish(session.id, Bust, messageQueues) else IO(game)
            } yield updatedGame
            else finish(session.id, Bust, messageQueues)
          case _ => IO(this)
        }
      case _ => IO(this)
    }
  }

  def finish(id: UUID, status: HandStatus, messageQueues: MessageQueues): IO[Game] = {
    players.get(id) match {
      case Some(player) =>
        player.currentHand match {
          case Some((currentHandKey, hand)) =>
            val message = ServerMessage.sendMessage(
              s"Score: ${hand.score}. Status: ${status}",
              messageQueues,
              Some(id)
            );

            message *> IO {
              player.nextHand match {
                case Some((nextHandKey, hand)) =>
                  val currentHand = currentHandKey -> hand.copy(status = status)
                  val nextHand = nextHandKey -> hand.copy(status = Current)

                  copy(players = players + (id -> player.copy(hands = player.hands + currentHand + nextHand)))
                case None =>
                  val currentHand = currentHandKey -> hand.copy(status = status)
                  val finishedPlayer = id -> player.copy(status = Finished, hands = player.hands + currentHand)

                  players.find(player => player._2.isWaiting) match {
                    case Some((id, player)) =>
                      copy(players = players + finishedPlayer + (id -> player.copy(status = Turn)))
                    case _ =>
                      copy(players = players + finishedPlayer)
                  }
              }
            }
          case _ => IO(this)
        }
      case _ => IO(this)
    }
  }

  def bet(id: UUID, amount: Float): IO[Game] = IO {
    players.get(id) match {
      case Some(player) if this.betsOpen && !player.isBetPlaced =>
        copy(players = players + (id -> player.adjustBet(amount).copy(status = BetPlaced)))
      case _ =>
        this
    }
  }

  def doubleDown(id: UUID, deck: Deck[IO], messageQueues: MessageQueues): IO[Game] = {
    players.get(id) match {
      case Some(player) if player.isTurn => player.currentHand match {
        case Some((key, hand)) if player.isFirstDecision => for {
          card <- deck.drawOne
          currentHand = hand.addCard(card)
          updatedPlayer = id -> player.adjustBet(player.bet).copy(hands = player.hands + (key -> currentHand))
          game = copy(players = players + updatedPlayer)
          updatedGame <- game.finish(id, if (currentHand.isBust) Bust else DoubleDown, messageQueues)
        } yield updatedGame
        case _ => IO(this)
      }
      case _ => IO(this)
    }
  }

  def split(id: UUID, deck: Deck[IO]): IO[Game] = {
    players.get(id) match {
      case Some(player) if player.isTurn => player.currentHand match {
        case Some((key, hand)) if hand.canSplit => for {
          cards <- deck.draw(2)

        } yield {
          val hand1 = Hand(hand.cards.take(1) ::: cards.take(1))
          val hand2 = Hand(hand.cards.takeRight(1) ::: cards.takeRight(1))
          val hands = player.addHand(hand1).hands + (key -> hand2.copy(status = Current))

          copy(players = players + (id -> player.adjustBet(player.bet / player.hands.size).copy(hands = hands)))
        }
        case _ => IO(this)
      }
      case _ => IO(this)
    }
  }

  def surrender(id: UUID, messageQueues: MessageQueues): IO[Game] = {
    players.get(id) match {
      case Some(player) if player.isFirstDecision => this.finish(id, Surrender, messageQueues)
      case _ => IO(this)
    }
  }

  def dealerDraw(card: Card): Game = copy(dealer = dealer.copy(hand = dealer.hand.addCard(card)))

  def turn(deck: Deck[IO], messageQueues: MessageQueues): IO[Game] = {
    if (finished)
      if (dealer.canDraw) for {
        card <- deck.drawOne
        game <- dealerDraw(card).turn(deck, messageQueues)
      } yield game
      else processResult
    else
      players.find(_._2.isTurn) match {
        case Some((id, player)) => player.currentHand match {
          case Some((_, hand)) => ServerMessage.sendMessage(s"You have ${hand.score}" , messageQueues, Some(id)) *> IO(this)
          case None => IO(this)
        }
        case None => IO(this)
      }
  }

  def processResult: IO[Game] = {
    val updatedPlayers = players.map {
      case (id, player) =>
        val win = player.balance + player.bet * player.hands.map(_._2.getPayoutRatio(dealer.hand)).sum

        (id, player.copy(status = Wait, balance = player.balance + win))
    }

    IO(copy(players = updatedPlayers))
  }}
