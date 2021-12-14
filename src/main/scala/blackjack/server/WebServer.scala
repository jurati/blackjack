package blackjack.server

import blackjack.entity.card.Deck
import blackjack.entity.game.{Action, Game}
import blackjack.entity.player.{Dealer, Hand, Stand}
import blackjack.server.ClientMessage.{Bet, Decision, GameStatus}
import blackjack.server.ServerMessage.GameState
import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import fs2.concurrent.{Queue}
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

  type MessageQueue = Queue[IO, ServerMessage]
  type MessageQueues = Map[UUID, MessageQueue]

  private val httpApp: IO[HttpApp[IO]] = for {
    refGame <- Ref.of[IO, Game]( Game(Dealer(Hand(List.empty)), Map.empty))
    refMessageQueues <- Ref.of[IO, MessageQueues](Map.empty)
    deck <- Deck.create
  } yield HttpRoutes.of[IO] {
    case GET -> Root / "blackjack" =>
      def process(gameState: Game, clientMessage: ClientMessage, session: Session): IO[Game] =
        for {
          messageQueues <- refMessageQueues.get
          gameStateAfterDecision <- clientMessage match {
            case GameStatus(1) => gameState.start(deck, messageQueues)
            case Decision(Action.Hit) => gameState.hit(session, deck, messageQueues)
            case Decision(Action.Stand) => gameState.finish(session.id, Stand, messageQueues)
            case Decision(Action.DoubleDown) => gameState.doubleDown(session.id, deck, messageQueues)
            case Decision(Action.Surrender) => gameState.surrender(session.id, messageQueues)
            case Bet(amount) => gameState.bet(session.id, amount)
          }
          newGameState <- gameStateAfterDecision.turn(deck, messageQueues)
          _ <- ServerMessage.updateGameState(newGameState, messageQueues)
        } yield  newGameState

      for {
        queue <- Queue.unbounded[IO, ServerMessage]
        session = Session(randomUUID)
        response <- WebSocketBuilder[IO].build(
          receive = queue.enqueue.compose[Stream[IO, WebSocketFrame]](_.collect {
            case WebSocketFrame.Text(message, _) => message
          }.evalMap { message => jawn.decode[ClientMessage](message) match {
            case Right(ClientMessage.Message(text)) => IO(ServerMessage.Message(text))
            case Right(clientMessage) => for {
              game <- refGame.get
              queues <- refMessageQueues.get
              (updatedGame, updatedQueues) =  session.connect(game, queues, queue)
              newGame <- process(updatedGame, clientMessage, session)
              _ <- refGame.update(_ => newGame)
              _ <- refMessageQueues.updateAndGet(_ => updatedQueues)
            } yield GameState(newGame)
          }}),
          send = queue.dequeue.map(state => WebSocketFrame.Text(state.asJson.noSpaces)),
          onClose = refGame.update(session.disconnectFromGame) *> refMessageQueues.update(session.disconnectFromQueue)
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