package com.ramsvidor.api

import cats.effect.*
import com.ramsvidor.app.Blockchain.Block
import com.ramsvidor.app.{Blockchain, Transaction}
import com.ramsvidor.crypto.ECDSA.Signature
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{Encoder, Json}
import org.http4s.*
import org.http4s.circe.*

object APISpec {
  case class FaucetRequest(publicKey: String, amount: BigDecimal)

  case class TransferRequest(payerPrivateKey: String, payerPublicKey: String, payeePublicKey: String, amount: BigDecimal)

  case class WalletCreationResponse(privateKey: String, publicKey: String)

  given EntityEncoder[IO, WalletCreationResponse] = jsonEncoderOf[IO, WalletCreationResponse]

  given EntityDecoder[IO, FaucetRequest] = jsonOf[IO, FaucetRequest]

  given EntityDecoder[IO, TransferRequest] = jsonOf[IO, TransferRequest]

  given transactionEncoder[F[_]]: Encoder[Transaction[F]] with {
    final def apply(transaction: Transaction[F]): Json = Json.obj(
      ("hash", Json.fromString(transaction.hash)),
      ("payer", Json.fromString(transaction.payer.value)),
      ("payee", Json.fromString(transaction.payee.value)),
      ("amount", Json.fromBigDecimal(transaction.amount)),
      ("signature", transaction.signature match {
        case Some(signature) => Json.fromString(signature.data)
        case None => Json.Null
      }),
      ("blockHash", transaction.blockHash match {
        case Some(blockHash) => Json.fromString(blockHash)
        case None => Json.Null
      })
    )
  }

  given blockEncoder[F[_]](using Encoder[Transaction[F]]): Encoder[Block[F]] with {
    final def apply(block: Block[F]): Json = Json.obj(
      ("hash", Json.fromString(block.hash)),
      ("merkleRoot", Json.fromString(block.merkleRoot)),
      ("previousHash", Json.fromString(block.previousHash)),
      ("transactions", block.transactions.asJson)
    )
  }
}