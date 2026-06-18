package io.github.alelk.pws.database.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Manages the SQLCipher passphrase for the application database.
 *
 * A random 32-byte key is generated on the first launch, stored in [EncryptedSharedPreferences]
 * (backed by Android Keystore), and returned as a UTF-8 hex string on every subsequent launch.
 * The passphrase never leaves the device and is independent of the asset decryption key.
 *
 * Security properties:
 * - Unique per device — copying the .db file from another device is useless without its Keystore.
 * - Survives app updates as long as the Keystore entry is not cleared.
 * - Lost only if the user does a factory reset or clears app data (in which case the DB is gone anyway).
 */
internal object KeyManager {
    private const val PREFS_NAME = "pws_db_security"
    private const val KEY_PREF = "db_passphrase"

    /**
     * Returns the SQLCipher passphrase as UTF-8 bytes, suitable for passing directly to
     * [net.zetetic.database.sqlcipher.SupportOpenHelperFactory].
     *
     * On the first call a 32-byte random key is generated and stored in [EncryptedSharedPreferences]
     * (backed by Android Keystore). All subsequent calls return the same key.
     */
    fun getOrCreatePassphrase(context: Context): ByteArray =
        getOrCreatePassphrase(createEncryptedPrefs(context))

    /** Overload for testing: accepts a plain [SharedPreferences] to avoid Keystore dependency. */
    internal fun getOrCreatePassphrase(prefs: SharedPreferences): ByteArray {
        val existing = prefs.getString(KEY_PREF, null)
        if (existing != null) return existing.toByteArray(Charsets.UTF_8)

        val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val hexPassphrase = rawKey.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(KEY_PREF, hexPassphrase).apply()
        return hexPassphrase.toByteArray(Charsets.UTF_8)
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
