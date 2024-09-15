package com.ramsvidor.crypto

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.ramsvidor.crypto.ECDSA.KeyPair
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class ECDSASpec extends AsyncWordSpec with AsyncIOSpec with Matchers {

  "ECDSA" should {
    "successfully generate a KeyPair" in {
      KeyPair.generate[IO].map { keyPair =>
        keyPair.privateKey.value.length should be > 0
        keyPair.publicKey.value.length should be > 0
      }
    }

    "successfully sign and verify a message" in {
      val message = "This is a test message"
      for {
        keyPair <- KeyPair.generate[IO]
        signature <- keyPair.sign(message)
        isValid <- keyPair.verify(message, signature)
      } yield {
        signature.data.length should be > 0
        isValid shouldBe true
      }
    }

    "fail to verify a message with a different signature" in {
      val message1 = "This is message one"
      val message2 = "This is message two"
      for {
        keyPair <- KeyPair.generate[IO]
        signature1 <- keyPair.sign(message1)
        isValid <- keyPair.verify(message2, signature1)
      } yield {
        isValid shouldBe false
      }
    }

    "serialize and deserialize a KeyPair successfully" in {
      for {
        originalKeyPair <- KeyPair.generate[IO]
        deserializedKeyPair <- KeyPair.deserialize[IO](originalKeyPair.privateKey.value, originalKeyPair.publicKey.value)
        message = "Test message"
        signature <- originalKeyPair.sign(message)
        isValid <- deserializedKeyPair.verify(message, signature)
      } yield {
        isValid shouldBe true
        originalKeyPair.privateKey.value shouldBe deserializedKeyPair.privateKey.value
        originalKeyPair.publicKey.value shouldBe deserializedKeyPair.publicKey.value
      }
    }

    "fail to deserialize an invalid public key" in {
      val validPrivateKey = "MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgwN6BfFPwF41FNIJIR5OKJSNM1N7Cp+15KylNl+hp2" +
        "sygBwYFK4EEAAqhRANCAARIgNygVPdBznn+MEcb/vryalAraX1EEFBv1/lPDgER/qd6oW8YEOSycCwMqxobZA6XxKyTkc2LP8cwRHZQZwOs"
      recoverToSucceededIf[RuntimeException] {
        KeyPair.deserialize[IO](validPrivateKey, "invalidKey").unsafeToFuture()
      }
    }

    "fail to sign with invalid private key" in {
      val validPublicKey = "024880dca054f741ce79fe30471bfefaf26a502b697d4410506fd7f94f0e0111fe"
      for {
        maybeKeyPair <- KeyPair.deserialize[IO]("invalidPrivateKey", validPublicKey).attempt
      } yield {
        maybeKeyPair.isLeft shouldBe true
      }
    }

    "successfully generate, sign, verify, and deserialize keys" in {
      val message = "Hello, blockchain!"

      for {
        // 1. Generate the KeyPair and verify the signature
        keyPair <- KeyPair.generate[IO]
        signature <- keyPair.sign(message)
        isValid <- keyPair.verify(message, signature)

        // 2. Deserialize the KeyPair and verify the signature
        recoveredKeypair <- KeyPair.deserialize[IO](keyPair.privateKey.value, keyPair.publicKey.value)
        isStillValid <- recoveredKeypair.verify(message, signature)

        // 3. Sign and verify the message again with the deserialized keyPair
        anotherSignature <- recoveredKeypair.sign(message)
        isValidAgain <- recoveredKeypair.verify(message, anotherSignature)

        // 4. Cross-verify signatures between the original and deserialized KeyPair
        isValidJustOnceMore <- keyPair.verify(message, anotherSignature)

      } yield {
        signature.data.length should be > 0
        anotherSignature.data.length should be > 0

        isValid shouldBe true
        isStillValid shouldBe true
        isValidAgain shouldBe true
        isValidJustOnceMore shouldBe true

        // Ensure that both key pairs are equivalent after serialization
        keyPair.publicKey shouldBe recoveredKeypair.publicKey
        keyPair.privateKey shouldBe recoveredKeypair.privateKey
        recoveredKeypair shouldBe keyPair
      }
    }

    "verify different keys produce different signatures" in {
      val message = "Hello, blockchain!"
      for {
        // Generate two distinct KeyPairs
        keyPair1 <- KeyPair.generate[IO]
        keyPair2 <- KeyPair.generate[IO]

        // Sign with both key pairs
        signature1 <- keyPair1.sign(message)
        signature2 <- keyPair2.sign(message)

        // Verify the signatures
        isValid1 <- keyPair1.verify(message, signature1)
        isValid2 <- keyPair2.verify(message, signature2)

        // Cross-verify (should fail)
        isCrossValid1 <- keyPair1.verify(message, signature2)
        isCrossValid2 <- keyPair2.verify(message, signature1)

      } yield {
        isValid1 shouldBe true
        isValid2 shouldBe true

        // Ensure that cross-verification between two key pairs fails
        isCrossValid1 shouldBe false
        isCrossValid2 shouldBe false

        // Ensure the signatures are not identical
        signature1 should not be signature2
      }
    }
  }
}
