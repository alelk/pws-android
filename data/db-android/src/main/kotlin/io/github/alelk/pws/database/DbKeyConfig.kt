package io.github.alelk.pws.database

internal object DbKeyConfig {
  /**
   * Reconstructs the asset decryption key from two XOR'd byte arrays baked in by Gradle.
   * Neither array alone reveals the key; no plaintext hex string appears in the DEX.
   */
  fun keyHex(): String {
    val masked = BuildConfig.DB_KEY_MASKED
    val mask = BuildConfig.DB_KEY_MASK
    return ByteArray(masked.size) { i -> (masked[i].toInt() xor mask[i].toInt()).toByte() }
      .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
  }
}
