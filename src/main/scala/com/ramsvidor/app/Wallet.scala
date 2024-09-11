package com.ramsvidor.app

import cats.effect.Async
import cats.syntax.all.*
import com.ramsvidor.app.Validation.{FundsDontBelongToWallet, ValidationResult}
import com.ramsvidor.crypto.ECDSA.{KeyPair, PublicKey}

final case class Wallet[F[_] : Async](private val keyPair: KeyPair[F]) {

  val publicKey: PublicKey[F] = keyPair.publicKey

  def sign(transaction: Transaction[F]): F[ValidationResult[Transaction[F]]] =
    if transaction.payer != publicKey then FundsDontBelongToWallet(transaction.hash).invalidNel.pure[F]
    else keyPair.sign(transaction.hash).map(transaction.updateSignature).map(_.validNel)

  override def toString: String =
    s"${getClass.getSimpleName}($publicKey)"

}

object Wallet {
  def apply[F[_] : Async](): F[Wallet[F]] =
    KeyPair.generate.map(Wallet.apply)

  def apply[F[_] : Async](privateKey: String, publicKey: String): F[Wallet[F]] =
    KeyPair.deserialize(privateKey, publicKey).map(Wallet.apply)
}
