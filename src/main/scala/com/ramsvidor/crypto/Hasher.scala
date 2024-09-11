package com.ramsvidor.crypto

import java.security.MessageDigest

object Hasher {

  def sha256Hash(data: String): String =
    MessageDigest.getInstance("SHA-256").digest(data.getBytes).map("%02x".format(_)).mkString

}
