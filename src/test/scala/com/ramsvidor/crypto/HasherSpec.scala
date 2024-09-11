package com.ramsvidor.crypto

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HasherSpec extends AnyWordSpec with Matchers {

  "Hasher" should {
    "correctly hash input using SHA-256" in {
      val input = "hello world"
      val expectedHash = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
      val actualHash = Hasher.sha256Hash(input)

      actualHash shouldEqual expectedHash
    }

    "produce different hashes for different inputs" in {
      val input1 = "hello world"
      val input2 = "goodbye world"

      val hash1 = Hasher.sha256Hash(input1)
      val hash2 = Hasher.sha256Hash(input2)

      hash1 should not equal hash2
    }

    "produce the same hash for the same input" in {
      val input = "hello world"

      val hash1 = Hasher.sha256Hash(input)
      val hash2 = Hasher.sha256Hash(input)

      hash1 shouldEqual hash2
    }
  }
}
