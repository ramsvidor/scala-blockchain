package com.ramsvidor.app

import cats.effect.Async
import com.ramsvidor.crypto.ECDSA.{PublicKey, Signature}
import com.ramsvidor.crypto.Hasher.sha256Hash

final case class Transaction[F[_] : Async](hash: String,
                                           payer: PublicKey[F],
                                           payee: PublicKey[F],
                                           amount: BigDecimal,
                                           signature: Option[Signature] = None,
                                           blockHash: Option[String] = None) {
  def isSigned: Boolean = signature.isDefined && signature.get.data.nonEmpty

  def isConfirmed: Boolean = blockHash.isDefined && blockHash.get.nonEmpty

  def updateBlockHash(blockHash: String): Transaction[F] =
    copy(blockHash = Option(blockHash))


  def updateSignature(signature: Signature): Transaction[F] =
    copy(signature = Option(signature))

  override def equals(obj: Any): Boolean = obj match {
    case that: Transaction[_] => hash == that.hash
    case _ => false
  }

  override def hashCode(): Int = hash.hashCode
}

object Transaction {
  def apply[F[_]](payer: PublicKey[F], payee: PublicKey[F], amount: BigDecimal)(using F: Async[F]): Transaction[F] =
    Transaction(sha256Hash(s"${payer.value}:${payee.value}:$amount"), payer, payee, amount)

  def updateBlockHash[F[_]](blockHash: String, vector: Vector[Transaction[F]])(using F: Async[F]): Vector[Transaction[F]] =
    vector.map(_.updateBlockHash(blockHash))
}
