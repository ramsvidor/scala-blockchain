package com.ramsvidor.app

import cats.data.ValidatedNel
import com.ramsvidor.crypto.Hasher.sha256Hash

import scala.annotation.tailrec

object Validation {
  type ValidationResult[A] = ValidatedNel[ValidationError, A]

  sealed trait ValidationError {
    def errorMessage: String
  }

  case class InsufficientFunds(txid: String) extends ValidationError {
    override def errorMessage: String = s"txid($txid): Insufficient funds"
  }

  case class TransactionInvalidAmount(txid: String) extends ValidationError {
    override def errorMessage: String = s"txid($txid): You must specify an amount greater than zero"
  }

  case class TransferToSameKey(txid: String) extends ValidationError {
    override def errorMessage: String = s"txid($txid): You cannot transfer funds to yourself"
  }

  case class FundsDontBelongToWallet(txid: String) extends ValidationError {
    override def errorMessage: String = s"txid($txid): The funds do not belong to the wallet"
  }

  case class NoTransactionsToMine() extends ValidationError {
    override def errorMessage: String = "There are no transactions to add to the ledger"
  }

  case class UnsignedTransaction(txid: String) extends ValidationError {
    override def errorMessage: String = s"txid($txid): Transactions must be signed prior to its broadcasting"
  }

  sealed trait MerkleTree {
    def hash: String
  }

  private case class Leaf(override val hash: String) extends MerkleTree

  private case class Branch(override val hash: String,
                            left: MerkleTree,
                            right: MerkleTree) extends MerkleTree

  object MerkleTree {
    def apply(transactions: Vector[Transaction[_]]): MerkleTree = {
      if (transactions.isEmpty) throw new IllegalArgumentException("Cannot build a Merkle tree with no transactions")
      buildTree(transactions.map(transaction => Leaf(transaction.hash)))
    }

    def verify(merkleRoot: String, transactions: Vector[Transaction[_]]): Boolean =
      MerkleTree(transactions).hash == merkleRoot

    @tailrec
    private def buildTree(leaves: Vector[MerkleTree]): MerkleTree = leaves match {
      case Vector(leaf) => leaf
      case _ =>
        val branches = leaves.grouped(2).map {
          case Vector(left, right) => Branch(sha256Hash(left.hash + right.hash), left, right)
          case Vector(leaf) => Branch(sha256Hash(leaf.hash + leaf.hash), leaf, leaf)
          case _ => throw new IllegalStateException("Unexpected group size in Merkle tree construction")
        }

        buildTree(branches.toVector)
    }
  }
}
