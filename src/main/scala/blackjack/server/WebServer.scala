package blackjack.server

import blackjack.entity.card.{Card, Deck}
import blackjack.entity.game.Action
import blackjack.entity.game.Game
import blackjack.entity.player.{Bust, Dealer, Hand, Player, Stand}
import blackjack.server.ClientMessage.{Decision, GameStatus}
import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp}
import fs2.{Pipe, Stream}
import fs2.concurrent.{Queue, Topic}
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
      def process(message: String, session: Session): IO[GameState] = {
        for {
          gameState <- refState.get
          newGameState <- {
            jawn.decode[ClientMessage](message) match {
              case Right(GameStatus(1)) => Game.startGame(refState, deck)
              case Right(Decision(Action.Hit)) =>
                if (gameState._2(session.id).canHit) Game.hit(refState, session.id, deck)
                else Game.finish(refState, session.id, Bust)
              case Right(Decision(Action.Stand)) => Game.finish(refState, session.id, Stand)
              case _ => IO(gameState)
            }
          }
          newGameState <-
            if (newGameState._2.forall(_._2.isFinished) && newGameState._1.canDraw) Game.dealerDraw(refState, deck)
            else IO(newGameState)
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