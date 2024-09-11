package com.ramsvidor.crypto

import cats.effect.Async
import cats.effect.implicits.*
import cats.syntax.all.*
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.nio.charset.StandardCharsets
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, KeyPairGenerator, Security}
import java.util.Base64

object ECDSA {
  Security.addProvider(new BouncyCastleProvider())

  private val base64Encoder = Base64.getEncoder
  private val base64Decoder = Base64.getDecoder

  private def keyFactory[F[_]](using F: Async[F]): F[KeyFactory] = Async[F].delay {
    KeyFactory.getInstance("ECDSA", "BC")
  }.handleErrorWith(raiseError[F, KeyFactory](_, "Failed to get key factory"))

  private def keyPairGenerator[F[_]](using F: Async[F]): F[KeyPairGenerator] = Async[F].delay {
    val generator = KeyPairGenerator.getInstance("ECDSA", "BC")
    generator.initialize(256)
    generator
  }.handleErrorWith(raiseError[F, KeyPairGenerator](_, "Failed to get key generator"))

  final case class Signature private(data: String)

  object Signature {
    def apply(data: Array[Byte]): Signature = Signature(base64Encoder.encodeToString(data))
  }

  final case class KeyPair[F[_]] private(privateKey: PrivateKey[F], publicKey: PublicKey[F])(using F: Async[F]) {
    def sign(message: String): F[Signature] =
      (privateKey.deserialize, getSigningInstance).parMapN { (signingKey, signingInstance) =>
        signingInstance.initSign(signingKey)
        signingInstance.update(message.getBytes(StandardCharsets.UTF_8))
        Signature(signingInstance.sign())
      }

    def verify(message: String, signature: Signature): F[Boolean] =
      KeyPair.verify(message, publicKey, signature)
  }

  object KeyPair {
    def generate[F[_]](using F: Async[F]): F[KeyPair[F]] = generateKeyPair.flatMap { keyPair =>
      (PrivateKey(keyPair.getPrivate), PublicKey(keyPair.getPublic)).mapN(KeyPair(_, _))
    }

    def deserialize[F[_]](serializedPrivateKey: String, serializedPublicKey: String)(using F: Async[F]): F[KeyPair[F]] = (
      deserializePrivateKey(serializedPrivateKey),
      deserializePublicKey(serializedPublicKey)
    ).parFlatMapN { (javaSecurityPrivateKey, javaSecurityPublicKey) =>
      (PrivateKey(javaSecurityPrivateKey), PublicKey(javaSecurityPublicKey)).parMapN(KeyPair(_, _))
    }

    def verify[F[_]](message: String, publicKey: PublicKey[F], signature: Signature)(using F: Async[F]): F[Boolean] =
      (publicKey.deserialize, getSigningInstance).parMapN { (verifyingKey, verifyingInstance) =>
        verifyingInstance.initVerify(verifyingKey)
        verifyingInstance.update(message.getBytes(StandardCharsets.UTF_8))
        verifyingInstance.verify(base64Decoder.decode(signature.data))
      }
  }

  sealed trait Key[F[_], K <: java.security.Key](using F: Async[F]) {
    def value: String

    def deserialize: F[K]
  }

  final case class PublicKey[F[_] : Async](override val value: String) extends Key[F, java.security.PublicKey] {
    override def deserialize: F[java.security.PublicKey] = deserializePublicKey(value)
  }

  object PublicKey {
    def apply[F[_]](key: java.security.PublicKey)(using F: Async[F]): F[PublicKey[F]] =
      Async[F].delay(PublicKey(base64Encoder.encodeToString(key.getEncoded)))

    def apply[F[_]](key: java.security.PublicKey, testKey: Boolean)(using F: Async[F]): PublicKey[F] =
      require(testKey, "Public key should be used for testing only") // scalastyle:ignore
      PublicKey(base64Encoder.encodeToString(key.getEncoded))
  }

  final case class PrivateKey[F[_] : Async] private(override val value: String) extends Key[F, java.security.PrivateKey] {
    override def deserialize: F[java.security.PrivateKey] = deserializePrivateKey(value)
  }

  object PrivateKey {
    def apply[F[_]](key: java.security.PrivateKey)(using F: Async[F]): F[PrivateKey[F]] =
      Async[F].delay(PrivateKey(base64Encoder.encodeToString(key.getEncoded)))

    def apply[F[_]](key: java.security.PrivateKey, testKey: Boolean)(using F: Async[F]): PrivateKey[F] =
      require(testKey, "Private key should be used for testing only") // scalastyle:ignore
      PrivateKey(base64Encoder.encodeToString(key.getEncoded))
  }

  protected def raiseError[F[_], A](e: Throwable, message: String = "ECDSA Error")(using F: Async[F]): F[A] =
    Async[F].raiseError(new RuntimeException(s"$message: ${e.getMessage}", e))

  private def decodeBase64Key(key: String): Array[Byte] =
    base64Decoder.decode(key)

  private def deserializePrivateKey[F[_]](key: String)(using F: Async[F]): F[java.security.PrivateKey] =
    keyFactory.map(_.generatePrivate(new PKCS8EncodedKeySpec(decodeBase64Key(key), "ECDSA")))
      .handleErrorWith(raiseError[F, java.security.PrivateKey](_, s"Failed to deserialize private key ${key.take(12)}"))

  private def deserializePublicKey[F[_]](key: String)(using F: Async[F]): F[java.security.PublicKey] =
    keyFactory.map(_.generatePublic(new X509EncodedKeySpec(decodeBase64Key(key), "ECDSA")))
      .handleErrorWith(raiseError[F, java.security.PublicKey](_, s"Failed to deserialize public key ${key.take(12)}"))

  private def getSigningInstance[F[_]](using F: Async[F]): F[java.security.Signature] =
    Async[F].delay(java.security.Signature.getInstance("SHA256withECDSA", "BC"))
      .handleErrorWith(raiseError[F, java.security.Signature](_, "Failed to get Signature instance"))

  private def generateKeyPair[F[_]](using F: Async[F]): F[java.security.KeyPair] =
    keyPairGenerator.map(_.generateKeyPair())
      .handleErrorWith(raiseError[F, java.security.KeyPair](_, "Failed to generate KeyPair"))
}
