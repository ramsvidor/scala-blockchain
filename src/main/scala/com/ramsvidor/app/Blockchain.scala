package com.ramsvidor.app

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.effect.{Async, Ref}
import cats.syntax.all.*
import com.ramsvidor.app.Blockchain.*
import com.ramsvidor.app.Validation.*
import com.ramsvidor.crypto.ECDSA.{KeyPair, PublicKey}
import com.ramsvidor.crypto.Hasher.sha256Hash

import scala.collection.immutable.Queue

final case class Blockchain[F[_] : Async](ref: Ref[F, Ledger[F]]) {

  private val faucetWallet = Wallet[F]()

  def currentState: F[Ledger[F]] = ref.get

  def updateState(): F[ValidationResult[Unit]] = currentState.flatMap { ledger =>
    if ledger.mempool.isEmpty then NoTransactionsToMine().invalidNel.pure[F]
    else for {
      validTransactions <- ledger.mempool.filterA(verifyTransaction).map(_.toVector)
      _ <- ref.update(_.mine(Block(ledger.blocks.lastOption.map(_.hash).getOrElse("genesis"), validTransactions)))
    } yield ().validNel
  }

  def requestFunds(publicKey: PublicKey[F], amount: BigDecimal): F[ValidationResult[Unit]] =
    if amount <= 0 then TransactionInvalidAmount("faucet-tx").invalidNel.pure[F]
    else for {
      wallet <- faucetWallet
      maybeValidTransaction <- wallet.sign(Transaction(wallet.publicKey, publicKey, amount))
      result <- maybeValidTransaction match {
        case Valid(tx) => broadcastTransaction(tx).map(_.validNel)
        case Invalid(e) => e.invalid[Unit].pure[F]
      }
    } yield result

  def transfer(transaction: Transaction[F]): F[ValidationResult[Unit]] =
    (balance(transaction.payer), unconfirmedBalance(transaction.payer)).flatMapN { (confirmed, unconfirmed) =>
      val totalBalance = confirmed + unconfirmed

      if transaction.amount <= 0 then TransactionInvalidAmount(transaction.hash).invalidNel.pure[F]
      else if transaction.payer == transaction.payee then TransferToSameKey(transaction.hash).invalidNel.pure[F]
      else if !transaction.isSigned then UnsignedTransaction(transaction.hash).invalidNel.pure[F]
      else if transaction.amount > totalBalance then InsufficientFunds(transaction.hash).invalidNel.pure[F]
      else broadcastTransaction(transaction).map(_.validNel)
    }

  def balance(publicKey: PublicKey[F]): F[BigDecimal] = currentState.map { ledger =>
    val transactions = ledger.blocks.flatMap(_.transactions)
    val received = transactions.filter(_.payee == publicKey).map(_.amount).sum
    val spent = transactions.filter(_.payer == publicKey).map(_.amount).sum

    received - spent
  }

  def unconfirmedBalance(publicKey: PublicKey[F]): F[BigDecimal] =
    currentState.map { ledger =>
      val received = ledger.mempool.filter(_.payee == publicKey).map(_.amount).sum
      val sent = ledger.mempool.filter(_.payer == publicKey).map(_.amount).sum

      received - sent
    }

  private def broadcastTransaction(transaction: Transaction[F]): F[Unit] =
    ref.update(_.queueToMempool(transaction))

  private def verifyTransaction(transaction: Transaction[F]): F[Boolean] =
    if transaction.isSigned then KeyPair.verify(transaction.hash, transaction.payer, transaction.signature.get)
    else false.pure[F]

}

object Blockchain {

  final case class Ledger[F[_] : Async](blocks: Vector[Block[F]],
                                        mempool: Queue[Transaction[F]]) {

    def queueToMempool(transaction: Transaction[F]): Ledger[F] =
      copy(mempool = mempool.enqueue(transaction))

    def mine(block: Block[F]): Ledger[F] = {
      copy(
        blocks = blocks :+ block.copy(transactions = Transaction.updateBlockHash(block.hash, block.transactions)),
        mempool = mempool.filterNot(block.transactions.contains))
    }
  }

  final case class Block[F[_] : Async](hash: String,
                                       merkleRoot: String,
                                       previousHash: String,
                                       transactions: Vector[Transaction[F]])

  object Block {

    def apply[F[_]](previousHash: String, transactions: Vector[Transaction[F]])(using F: Async[F]): Block[F] =
      Block(sha256Hash(s"$previousHash:${MerkleTree(transactions).hash}:${transactions.map(_.hash).mkString}"),
        MerkleTree(transactions).hash,
        previousHash,
        transactions)
  }
}
