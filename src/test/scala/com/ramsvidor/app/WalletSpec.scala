package com.ramsvidor.app

import cats.data.Validated.Invalid
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.ramsvidor.app.Validation.FundsDontBelongToWallet
import com.ramsvidor.crypto.ECDSA.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class WalletSpec extends AsyncWordSpec with AsyncIOSpec with Matchers {

  "Wallet" should {
    "generate a new wallet with valid key pair" in {
      Wallet[IO]().map { wallet =>
        wallet.publicKey.value.nonEmpty shouldEqual true
      }
    }

    "sign a transaction and update the signature" in {
      for {
        wallet <- Wallet[IO]()
        sampleTransaction = Transaction(
          payer = wallet.publicKey,
          payee = wallet.publicKey,
          amount = 100
        )
        validatedTransaction <- wallet.sign(sampleTransaction).map(_.toEither)
        signedTransaction = validatedTransaction.getOrElse(fail("Expected a valid transaction"))
      } yield signedTransaction.isSigned shouldEqual true
    }

    "sign a transaction that doesn't belong to the wallet" in {
      for {
        wallet <- Wallet[IO]()
        thirdPartyWallet <- Wallet[IO]()
        sampleTransaction = Transaction(
          payer = thirdPartyWallet.publicKey,
          payee = wallet.publicKey,
          amount = 100
        )
        result <- wallet.sign(sampleTransaction)
      } yield result match {
        case Invalid(errors) => errors.head shouldBe a[FundsDontBelongToWallet]
        case _ => fail("Expected FundsDontBelongToWallet error")
      }
    }

    "deserialize a wallet from given private and public keys" in {
      for {
        keyPair <- KeyPair.generate[IO]
        serializedPrivateKey = keyPair.privateKey.value
        serializedPublicKey = keyPair.publicKey.value
        deserializedWallet <- Wallet[IO](serializedPrivateKey, serializedPublicKey)
      } yield {
        deserializedWallet.publicKey shouldEqual keyPair.publicKey
      }
    }
  }
}