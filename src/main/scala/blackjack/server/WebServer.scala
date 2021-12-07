package blackjack.server

import blackjack.entity.card.Deck
import blackjack.entity.game.Action
import blackjack.entity.game.Game
import blackjack.entity.player.{BetPlaced, Bust, Dealer, Hand, Player, Stand, Surrender, Wait}
import blackjack.server.ClientMessage.{Bet, Decision, GameStatus}
import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import fs2.concurrent.Topic
import io.circe.jawn
import io.circe.syntax.EncoderOps
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import java.util.UUID
import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext

object WebServer extends IOApp {
  import JsonCodec._
  //todo: think about Map[UUID, Queue[Player]]
  type GameState = (Dealer, Map[UUID, Player])

  private val httpApp: IO[HttpApp[IO]] = for {
    refState <- Ref.of[IO, GameState]((Dealer(Hand(List.empty)), Map.empty))
    deck <- Deck.create
    gameState <- refState.get
    topic <- Topic[IO, GameState](gameState)
  } yield HttpRoutes.of[IO] {
    case GET -> Root / "blackjack" =>
      def processStartGame(gameState: GameState): IO[GameState] = {
        if (gameState._2.forall(_._2.status == BetPlaced))
          deck.draw(1 + gameState._2.size * 2).map(cards => Game.startGame(gameState, cards))
        else
          IO(gameState)
      }

      def processDecisionHit(gameState: GameState, session: Session): IO[GameState] = {
        gameState._2.get(session.id) match {
          case Some(player) =>
            if (player.canHit) deck.drawOne.map(card => Game.hit(gameState, session.id, card))
            else IO(Game.finish(gameState, session.id, Bust))
          case None => IO(gameState)
        }
      }

      def processDecisionStand(gameState: GameState, session: Session): IO[GameState] =
        IO(Game.finish(gameState, session.id, Stand))

      def processDecisionDoubleDown(gameState: GameState, session: Session): IO[GameState] =
        gameState._2.get(session.id) match {
          case Some(player) =>
            if (player.canDoubleDown) deck.drawOne.map(card => Game.doubleDown(gameState, session.id, card))
            else IO(gameState)
          case None => IO(gameState)
        }

      def processDecisionSurrender(gameState: GameState, session: Session): IO[GameState] =
        IO(Game.surrender(gameState, session.id))

      def processBet(gameState: GameState, session: Session, amount: Float): IO[GameState] =
        IO(Game.bet(gameState, session.id, amount))

      def processDealerTurn(gameState: GameState): IO[GameState] = {
        if (Game.finished(gameState))
          if (gameState._1.canDraw) deck.drawOne.map(card => Game.dealerDraw(gameState, card))
          else processResult(gameState)
        else
          IO(gameState)
      }

      def processResult(gameState: GameState): IO[GameState] = {
        val (dealer, players) = gameState

        val newPlayers = players.map {
          case (id, player) if player.status != Surrender =>
            (id, Player(player.hand, Wait, player.balance + player.bet * player.hand.getPayoutRatio(dealer.hand), 0))
        }

        IO((dealer, newPlayers))
      }

      def process(message: String, session: Session): IO[GameState] = {
        for {
          gameState <- refState.get
          newGameState <-
            jawn.decode[ClientMessage](message) match {
              case Right(GameStatus(1)) => processStartGame(gameState)
              case Right(Decision(Action.Hit)) => processDecisionHit(gameState, session)
              case Right(Decision(Action.Stand)) => processDecisionStand(gameState, session)
              case Right(Decision(Action.DoubleDown)) => processDecisionDoubleDown(gameState, session)
              case Right(Decision(Action.Surrender)) => processDecisionSurrender(gameState, session)
              case Right(Bet(amount)) => processBet(gameState, session, amount)
              case _ => IO(gameState)
            }
          newGameState <- processDealerTurn(newGameState)
          newGameState <- refState.updateAndGet(_ => newGameState)
        } yield newGameState
      }

      for {
       session <- IO(Session(randomUUID))
       _ <- refState.update(session.connect)
       response <- WebSocketBuilder[IO].build(
          receive = topic.publish.compose[Stream[IO, WebSocketFrame]](_.collect {
            case WebSocketFrame.Text(message, _) => message
          }.evalMap(process(_, session))),
          send = topic.subscribe(maxQueued = 2).map(state => WebSocketFrame.Text(state.asJson.noSpaces)),
          onClose = refState.update(session.disconnect)
        )
      } yield response
  }.orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    for {
      httpApp <- httpApp
      _ <- BlazeServerBuilder[IO](ExecutionContext.global)
        .bindHttp(port = 9001, host = "localhost")
        .withHttpApp(httpApp)
        .serve
        .compile
        .drain
    } yield ExitCode.Success
}