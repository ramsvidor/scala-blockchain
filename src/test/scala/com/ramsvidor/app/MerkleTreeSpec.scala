package com.ramsvidor.app

import cats.effect.IO
import com.ramsvidor.app.Validation.MerkleTree
import com.ramsvidor.crypto.ECDSA.PublicKey
import com.ramsvidor.crypto.Hasher.sha256Hash
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MerkleTreeSpec extends AnyWordSpec with Matchers {

  private val sampleTransactions = Vector(
    createTransaction(100),
    createTransaction(200),
    createTransaction(300),
    createTransaction(400)
  )

  def createTransaction(amount: BigDecimal): Transaction[IO] = {
    Transaction(PublicKey(System.nanoTime().toString), PublicKey(System.nanoTime().toString), amount)
  }

  "MerkleTree" should {
    "correctly build a tree with an even number of transactions" in {
      val merkleTree = MerkleTree(sampleTransactions)
      val expectedRootHash = sha256Hash(
        sha256Hash(sampleTransactions(0).hash + sampleTransactions(1).hash) +
          sha256Hash(sampleTransactions(2).hash + sampleTransactions(3).hash)
      )

      merkleTree.hash shouldEqual expectedRootHash
    }

    "correctly build a tree with an odd number of transactions by duplicating the last leaf" in {
      val transactions = Vector(
        createTransaction(100),
        createTransaction(200),
        createTransaction(300)
      )

      val merkleTree = MerkleTree(transactions)
      val expectedRootHash = sha256Hash(
        sha256Hash(transactions(0).hash + transactions(1).hash) +
          sha256Hash(transactions(2).hash + transactions(2).hash)
      )

      merkleTree.hash shouldEqual expectedRootHash
    }

    "verify the Merkle root hash correctly" in {
      val merkleTree = MerkleTree(sampleTransactions)
      val isValid = MerkleTree.verify(merkleTree.hash, sampleTransactions)

      isValid shouldEqual true
    }

    "fail to verify the Merkle root with a modified transaction" in {
      val transactions = Vector(
        createTransaction(100),
        createTransaction(200)
      )

      val modifiedTransactions = transactions.updated(1, createTransaction(300))

      val merkleTree = MerkleTree(transactions)
      val isValid = MerkleTree.verify(merkleTree.hash, modifiedTransactions)

      isValid shouldEqual false
    }

    "handle an empty transaction list" in {
      a[IllegalArgumentException] shouldBe thrownBy(MerkleTree(Vector.empty))
    }

    "handle an empty transaction list in verification" in {
      a[IllegalArgumentException] shouldBe thrownBy(MerkleTree.verify("somehash", Vector.empty))
    }
  }
}