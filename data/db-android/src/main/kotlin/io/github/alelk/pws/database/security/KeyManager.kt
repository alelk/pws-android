package io.github.alelk.pws.database.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the SQLCipher passphrase for the application database.
 *
 * A random 32-byte key is generated on the first launch, encrypted with an AES-256-GCM key
 * stored in Android Keystore, and persisted in SharedPreferences. On every subsequent launch
 * the key is decrypted and returned as a UTF-8 hex string.
 *
 * Security properties:
 * - Unique per device — copying the .db file from another device is useless without its Keystore.
 * - Survives app updates as long as the Keystore entry is not cleared.
 * - Lost only if the user does a factory reset or clears app data (in which case the DB is gone anyway).
 *
 * Migration: on first launch after upgrading from the EncryptedSharedPreferences-based storage
 * (v1), the legacy passphrase is read from the old prefs file and re-encrypted in the new format
 * so that existing databases remain accessible.
 */
internal object KeyManager {
    private const val PREFS_NAME = "pws_db_security_v2"
    private const val LEGACY_PREFS_NAME = "pws_db_security"
    private const val KEY_PREF = "db_passphrase"
    private const val KEYSTORE_ALIAS = "pws_db_master_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    /**
     * Returns the SQLCipher passphrase as UTF-8 bytes, suitable for passing directly to
     * [net.zetetic.database.sqlcipher.SupportOpenHelperFactory].
     *
     * On the first call a 32-byte random key is generated, encrypted with the Keystore-backed
     * AES-256-GCM key, and stored in SharedPreferences. All subsequent calls decrypt and return
     * the same passphrase.
     */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val secretKey = getOrCreateKeystoreKey()

        val stored = prefs.getString(KEY_PREF, null)
        if (stored != null) return decrypt(Base64.decode(stored, Base64.DEFAULT), secretKey)

        // One-time migration from EncryptedSharedPreferences (v1 storage).
        val passphrase = readLegacyPassphrase(context) ?: generatePassphrase()
        storePassphrase(prefs, passphrase, secretKey)
        return passphrase.toByteArray(Charsets.UTF_8)
    }

    /** Overload for testing: accepts a plain [SharedPreferences] to avoid Keystore dependency. */
    internal fun getOrCreatePassphrase(prefs: SharedPreferences): ByteArray {
        val existing = prefs.getString(KEY_PREF, null)
        if (existing != null) return existing.toByteArray(Charsets.UTF_8)

        val passphrase = generatePassphrase()
        prefs.edit().putString(KEY_PREF, passphrase).apply()
        return passphrase.toByteArray(Charsets.UTF_8)
    }

    // Reads the passphrase stored by the previous EncryptedSharedPreferences-based implementation.
    // Failures (missing Keystore entry, corrupted file, etc.) are silently swallowed so a fresh
    // passphrase is generated instead of crashing — the user's database would be inaccessible in
    // that case, but the app remains functional.
    @Suppress("DEPRECATION")
    private fun readLegacyPassphrase(context: Context): String? = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            LEGACY_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).getString(KEY_PREF, null)
    }.getOrNull()

    private fun generatePassphrase(): String {
        val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return rawKey.joinToString("") { "%02x".format(it) }
    }

    private fun storePassphrase(prefs: SharedPreferences, passphrase: String, key: SecretKey) {
        val encrypted = Base64.encodeToString(encrypt(passphrase.toByteArray(Charsets.UTF_8), key), Base64.DEFAULT)
        prefs.edit().putString(KEY_PREF, encrypted).apply()
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }
        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private fun encrypt(plaintext: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv  // GCM auto-generates a 12-byte IV
        return iv + cipher.doFinal(plaintext)
    }

    private fun decrypt(ivAndCiphertext: ByteArray, key: SecretKey): ByteArray {
        val iv = ivAndCiphertext.copyOf(GCM_IV_BYTES)
        val ciphertext = ivAndCiphertext.copyOfRange(GCM_IV_BYTES, ivAndCiphertext.size)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }
}
