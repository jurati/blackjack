package blackjack.server

import blackjack.entity.game.Game
import blackjack.server.WebServer.MessageQueues
import cats.effect.{ContextShift, IO}
import cats.implicits.catsSyntaxParallelSequence

import java.util.UUID
import scala.concurrent.ExecutionContext.global

sealed trait ServerMessage

object ServerMessage {
  final case class GameState(game: Game) extends ServerMessage
  final case class Message(text: String) extends ServerMessage

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)

  def updateGameState(game: Game, messageQueues: MessageQueues): IO[Unit] =
    messageQueues.map(_._2.enqueue1(ServerMessage.GameState(game))).toVector.parSequence.void

  def sendMessage(message: String, messageQueues: MessageQueues, id: Option[UUID] = None): IO[Unit] = {
    val serverMessage = ServerMessage.Message(message)

    id match {
      case Some(id) => messageQueues.get(id) match {
        case Some(queue) => queue.enqueue1(serverMessage)
        case None => IO(())
      }
      case None => messageQueues.map(_._2.enqueue1(serverMessage)).toVector.parSequence.void
    }
  }
}
