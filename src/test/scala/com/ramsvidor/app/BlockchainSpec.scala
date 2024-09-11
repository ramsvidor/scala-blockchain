package com.ramsvidor.app

import cats.data.Validated.Invalid
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Ref}
import com.ramsvidor.app.Blockchain.Ledger
import com.ramsvidor.app.Validation.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.collection.immutable.Queue

class BlockchainSpec extends AsyncWordSpec with AsyncIOSpec with Matchers {

  private val initialLedger = Ledger[IO](Vector.empty, Queue.empty)

  "Blockchain" should {

    "reject fund requests with zero" in {
      for {
        wallet <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        result <- blockchain.requestFunds(wallet.publicKey, BigDecimal(0))
      } yield result match {
        case Invalid(errors) => errors.head shouldBe a[TransactionInvalidAmount]
        case _ => fail("Expected TransactionInvalidAmount error")
      }
    }

    "reject fund requests with negative" in {
      for {
        wallet <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        result <- blockchain.requestFunds(wallet.publicKey, BigDecimal(-50))
      } yield result match {
        case Invalid(errors) => errors.head shouldBe a[TransactionInvalidAmount]
        case _ => fail("Expected TransactionInvalidAmount error")
      }
    }

    "allow valid fund requests via faucet" in {
      for {
        wallet <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        _ <- blockchain.requestFunds(wallet.publicKey, BigDecimal(100))
        _ <- blockchain.updateState()
        balance <- blockchain.balance(wallet.publicKey)
      } yield balance shouldEqual 100
    }

    "update state should add blocks to the ledger" in {
      for {
        wallet <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        result <- blockchain.requestFunds(wallet.publicKey, BigDecimal(100))
        _ <- blockchain.updateState()
        currentState <- blockchain.currentState
      } yield currentState.blocks.size shouldEqual 1
    }

    "broadcast a signed transaction to the mempool via transfer" in {
      for {
        payer <- Wallet[IO]()
        payee <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        _ <- blockchain.requestFunds(payer.publicKey, BigDecimal(100))
        eitherTransaction <- payer.sign(Transaction(payer.publicKey, payee.publicKey, BigDecimal(50))).map(_.toEither)
        transaction = eitherTransaction.getOrElse(fail("Expected a valid transaction"))
        _ <- blockchain.transfer(transaction)
        state <- blockchain.currentState
      } yield state.mempool.contains(transaction) shouldEqual true
    }

    "mine a block and update the state" in {
      for {
        payer <- Wallet[IO]()
        payee <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        eitherTransaction1 <- payer.sign(Transaction(payer.publicKey, payee.publicKey, BigDecimal(50)))
        eitherTransaction2 <- payee.sign(Transaction(payee.publicKey, payer.publicKey, BigDecimal(10)))
        transaction1 = eitherTransaction1.getOrElse(fail("Expected a valid transaction"))
        transaction2 = eitherTransaction2.getOrElse(fail("Expected a valid transaction"))
        _ <- blockchain.requestFunds(payer.publicKey, BigDecimal(100))
        _ <- blockchain.updateState()
        _ <- blockchain.transfer(transaction1)
        _ <- blockchain.transfer(transaction2)
        _ <- blockchain.updateState()
        state <- blockchain.currentState
      } yield {
        state.blocks should have length 2
        state.blocks(1).transactions should contain allOf(transaction1, transaction2)
        state.mempool shouldBe empty
      }
    }

    "correctly calculate the balance of a public key" in {
      for {
        payer <- Wallet[IO]()
        payee <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        eitherTransaction1 <- payer.sign(Transaction(payer.publicKey, payee.publicKey, BigDecimal(50)))
        eitherTransaction2 <- payee.sign(Transaction(payee.publicKey, payer.publicKey, BigDecimal(10)))
        transaction1 = eitherTransaction1.getOrElse(fail("Expected a valid transaction"))
        transaction2 = eitherTransaction2.getOrElse(fail("Expected a valid transaction"))
        _ <- blockchain.requestFunds(payer.publicKey, BigDecimal(100))
        _ <- blockchain.updateState()
        _ <- blockchain.transfer(transaction1)
        _ <- blockchain.transfer(transaction2)
        _ <- blockchain.updateState()
        payerBalance <- blockchain.balance(payer.publicKey)
        payeeBalance <- blockchain.balance(payee.publicKey)
      } yield {
        payeeBalance shouldEqual 40
        payerBalance shouldEqual 60
      }
    }

    "correctly calculate unconfirmed balance" in {
      for {
        payer <- Wallet[IO]()
        payee <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        _ <- blockchain.requestFunds(payer.publicKey, BigDecimal(100))
        _ <- blockchain.updateState()
        eitherTransaction <- payer.sign(Transaction(payer.publicKey, payee.publicKey, BigDecimal(50)))
        transaction = eitherTransaction.getOrElse(fail("Expected a valid transaction"))
        _ <- blockchain.transfer(transaction)
        balance <- blockchain.unconfirmedBalance(payee.publicKey)
      } yield balance shouldEqual BigDecimal(50)
    }

    "fail to transfer if funds are insufficient" in {
      for {
        payer <- Wallet[IO]()
        payee <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        eitherTransaction <- payer.sign(Transaction(payer.publicKey, payee.publicKey, BigDecimal(100)))
        newTransaction = eitherTransaction.getOrElse(fail("Expected a valid transaction"))
        result <- blockchain.transfer(newTransaction)
      } yield result match {
        case Invalid(errors) => errors.head shouldBe a[InsufficientFunds]
        case _ => fail("Expected InsufficientFunds error")
      }
    }

    "reject unsigned transaction" in {
      for {
        payer <- Wallet[IO]()
        payee <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        result <- blockchain.transfer(Transaction(payer.publicKey, payee.publicKey, BigDecimal(100)))
      } yield result match {
        case Invalid(errors) => errors.head shouldBe a[UnsignedTransaction]
        case _ => fail("Expected UnsignedTransaction error")
      }
    }

    "reject transfers with zero amount" in {
      for {
        payer <- Wallet[IO]()
        payee <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        validatedTransaction <- payer.sign(Transaction(payer.publicKey, payee.publicKey, BigDecimal(0))).map(_.toEither)
        transaction = validatedTransaction.getOrElse(fail("Expected a valid transaction"))
        result <- blockchain.transfer(transaction)
      } yield result match {
        case Invalid(errors) => errors.head shouldBe a[TransactionInvalidAmount]
        case _ => fail("Expected TransactionInvalidAmount error")
      }
    }

    "reject transfers to the same public key" in {
      for {
        wallet <- Wallet[IO]()
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        validatedTransaction <- wallet.sign(Transaction(wallet.publicKey, wallet.publicKey, BigDecimal(50))).map(_.toEither)
        transaction = validatedTransaction.getOrElse(fail("Expected a valid transaction"))
        result <- blockchain.transfer(transaction)
      } yield result match {
        case Invalid(errors) => errors.head shouldBe a[TransferToSameKey]
        case _ => fail("Expected TransferToSameKey error")
      }
    }

    "mine empty mempool should fail" in {
      for {
        ref <- Ref.of[IO, Ledger[IO]](initialLedger)
        blockchain = Blockchain(ref)
        result <- blockchain.updateState()
      } yield result match {
        case Invalid(errors) => errors.head shouldBe a[NoTransactionsToMine]
        case _ => fail("Expected NoTransactionsToMine error")
      }
    }
  }
}
