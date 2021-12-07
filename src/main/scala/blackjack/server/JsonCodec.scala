package blackjack.server

import blackjack.entity.card.{Card, Rank, Suit}
import blackjack.entity.game.Action
import blackjack.entity.player.{Dealer, Hand, Player, Status}
import blackjack.server.ClientMessage.{Decision, GameStatus}
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec

object JsonCodec {
  implicit val CardCodec: Codec[Card] = deriveCodec[Card]
  implicit val dealerCodec: Codec[Dealer] = deriveCodec[Dealer]
  implicit val handCodec: Codec[Hand] = deriveCodec[Hand]
  implicit val playerCodec: Codec[Player] = deriveCodec[Player]
  implicit val actionCodec: Codec[Action] = deriveCodec[Action]
  implicit val decisionCodec: Codec[Decision] = deriveCodec[Decision]
  implicit val gameStatusCodec: Codec[GameStatus] = deriveCodec[GameStatus]
  implicit val clientMessageCodec: Codec[ClientMessage] = deriveCodec[ClientMessage]
  implicit val statusMessageCodec: Codec[Status] = deriveCodec[Status]

  implicit val rankDecoder: Decoder[Rank] =
    Decoder.forProduct2("value", "symbol")((_: String, symbol: String) => Rank.unsafeOf(symbol))
  implicit val rankEncoder: Encoder[Rank] =
    Encoder.forProduct2("value", "symbol")(r => (r.value, r.symbol))

  implicit val suitDecoder: Decoder[Suit] = Decoder.forProduct1("name")(Suit.unsafeOf)
  implicit val suitEncoder: Encoder[Suit] = Encoder.forProduct1("name")(_.name)
}
