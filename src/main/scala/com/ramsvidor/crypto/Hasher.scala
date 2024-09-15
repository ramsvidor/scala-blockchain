package com.ramsvidor.crypto

import java.security.MessageDigest

object Hasher {
  type HexString = String

  def sha256Hash(data: String): String =
    MessageDigest.getInstance("SHA-256").digest(data.getBytes)

  given byteArrayToString: Conversion[Array[Byte], String] with
    def apply(bytes: Array[Byte]): String =
      bytes.map(b => f"$b%02x").mkString

  given hexstringToByteArray: Conversion[HexString, Array[Byte]] with
    def apply(hexString: HexString): Array[Byte] =
      hexString.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
}
