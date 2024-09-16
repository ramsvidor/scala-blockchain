package com.ramsvidor.api

import cats.data.Validated.{Invalid, Valid}
import cats.effect.*
import com.ramsvidor.api.APISpec.{*, given}
import com.ramsvidor.app.{Blockchain, Transaction, Wallet}
import com.ramsvidor.crypto.ECDSA.{KeyPair, PublicKey}
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.io.*

object BlockchainAPI {
  def routes(blockchain: Blockchain[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case POST -> Root / "wallet" => for {
      keyPair <- KeyPair.generate[IO]
      wallet <- Wallet[IO](keyPair.privateKey.value, keyPair.publicKey.value)
      resp <- Ok(WalletCreationResponse(keyPair.privateKey.value, keyPair.publicKey.value).asJson)
    } yield resp

    case req@POST -> Root / "faucet" => req.as[FaucetRequest].flatMap { faucetReq =>
      blockchain.requestFunds(PublicKey[IO](faucetReq.publicKey), faucetReq.amount).flatMap {
        case Valid(_) => Ok(s"Funds transferred to wallet ${faucetReq.publicKey}")
        case Invalid(errors) => BadRequest(errors.toList.map(_.errorMessage).mkString(", "))
      }
    }

    case req@POST -> Root / "transfer" => req.as[TransferRequest].flatMap { transferReq =>
      for {
        payerWallet <- Wallet[IO](transferReq.payerPrivateKey, transferReq.payerPublicKey)
        transaction = Transaction[IO](payerWallet.publicKey, PublicKey(transferReq.payeePublicKey), transferReq.amount)
        maybeTransfer <- payerWallet.sign(transaction)
        transfer = maybeTransfer.getOrElse(transaction)
        result <- blockchain.transfer(transfer)
        resp <- result match {
          case Valid(_) => Ok("Transfer successful")
          case Invalid(errors) => BadRequest(errors.toList.map(_.errorMessage).mkString(", "))
        }
      } yield resp
    }

    case POST -> Root / "mine" => blockchain.updateState().flatMap {
      case Valid(_) => Ok("Blockchain mined successfully")
      case Invalid(errors) => BadRequest(errors.toList.map(_.errorMessage).mkString(", "))
    }

    case GET -> Root / "wallet" / publicKey / "balance" =>
      blockchain.balance(PublicKey(publicKey)).flatMap(balance => Ok(balance.toString))

    case GET -> Root / "wallet" / publicKey / "unconfirmed" =>
      blockchain.unconfirmedBalance(PublicKey(publicKey)).flatMap(balance => Ok(balance.toString))

    case GET -> Root / "transaction" / hash => blockchain.currentState.flatMap { ledger =>
      ledger.blocks.flatMap(_.transactions).find(_.hash == hash) match {
        case Some(transaction) => Ok(transaction.asJson)
        case None => NotFound(s"Transaction $hash not found")
      }
    }

    case GET -> Root / "block" => blockchain.currentState.flatMap(ledger => Ok(ledger.blocks.asJson))

    case GET -> Root / "block" / hash => blockchain.currentState.flatMap { ledger =>
      ledger.blocks.find(_.hash == hash) match {
        case Some(block) => Ok(block.asJson)
        case None => NotFound(s"Block $hash not found")
      }
    }

    case GET -> Root / "mempool" => blockchain.currentState.flatMap(ledger => Ok(ledger.mempool.toList.asJson))
  }
}
