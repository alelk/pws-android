package io.github.alelk.pws.database

import android.content.Context
import io.github.alelk.pws.database.PwsDatabaseProvider.DATABASE_NAME
import io.github.alelk.pws.portable.serialization.BundleCrypto
import java.util.zip.GZIPInputStream
import net.zetetic.database.sqlcipher.SQLiteDatabase as SQLCipherDatabase
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

private const val ASSETS_DB_FOLDER = "db"

/**
 * Initialises the database from an encrypted asset on the first launch.
 *
 * Expected asset: `db/<name>-<DB_VERSION>.dbz.enc`
 *   Wire format: PWSB( gzip(.db) )
 *   i.e. BundleCrypto.encrypt( gzip(.db) ) — the plaintext is a GZIP-compressed SQLite file.
 *
 * Steps:
 *   1. Decrypt .dbz.enc with [BuildConfig.DB_DECRYPT_KEY] → plain SQLite bytes (in memory).
 *   2. Write plain bytes to a temp file in cacheDir.
 *   3. Use SQLCipher's `sqlcipher_export` to copy the plain temp DB into the encrypted final DB.
 *   4. Delete the temp file.
 *
 * The [passphrase] parameter is the SQLCipher passphrase produced by
 * [io.github.alelk.pws.database.security.KeyManager.getOrCreatePassphrase].
 */
internal fun initDatabase(context: Context, passphrase: ByteArray) {
  val dbFile = context.getDatabasePath(DATABASE_NAME)
  if (dbFile.exists() && dbFile.isFile) {
    Timber.d("found existing database $dbFile")
    return
  }

  Timber.i("database $dbFile not found — initialising from assets/$ASSETS_DB_FOLDER ...")

  val am = context.assets
  val dbFolder = checkNotNull(dbFile.parentFile) { "cannot resolve parent dir of $dbFile" }
  if (!dbFolder.exists()) dbFolder.mkdirs()

  val fileList = checkNotNull(am.list(ASSETS_DB_FOLDER)) { "no files in assets/$ASSETS_DB_FOLDER" }
  val expectedSuffix = "-${PwsDatabaseProvider.DB_VERSION}.dbz.enc"
  val encAsset = fileList.firstOrNull { it.endsWith(expectedSuffix) }
    ?: error(
      "no asset matching *$expectedSuffix found in assets/$ASSETS_DB_FOLDER " +
        "(found: ${fileList.toList()}). Make sure ./fetch-db.sh was run for this variant."
    )

  Timber.i("found encrypted asset: $encAsset")

  val masterKey = BundleCrypto.keyFromHex(DbKeyConfig.keyHex())
  val tmpFile = File(context.cacheDir, "pws_init_tmp.db")

  try {
    val encBytes = am.open("$ASSETS_DB_FOLDER/$encAsset").use { it.readBytes() }

    Timber.i("decrypting $encAsset (${encBytes.size} bytes) ...")
    val gzippedDb = BundleCrypto.decrypt(encBytes, masterKey)

    Timber.i("decompressing gzip → temp file ...")
    val dbBytes = GZIPInputStream(gzippedDb.inputStream()).use { it.readBytes() }
    FileOutputStream(tmpFile).use { it.write(dbBytes) }

    Timber.i("encrypting → $dbFile via sqlcipher_export ...")
    val passphraseStr = passphrase.toString(Charsets.UTF_8)
    // Open the plain temp file with SQLCipher using empty key (plain mode)
    val tmpDb = SQLCipherDatabase.openDatabase(tmpFile.absolutePath, ByteArray(0), null, SQLCipherDatabase.OPEN_READWRITE, null)
    try {
      tmpDb.execSQL("ATTACH DATABASE '${dbFile.absolutePath}' AS encrypted KEY '$passphraseStr'")
      tmpDb.execSQL("SELECT sqlcipher_export('encrypted')")
      tmpDb.execSQL("DETACH DATABASE encrypted")
    } finally {
      tmpDb.close()
    }
  } catch (e: Exception) {
    Timber.e(e, "failed to initialise database from asset $encAsset: ${e.message}")
    if (dbFile.exists()) dbFile.delete()
    throw e
  } finally {
    if (tmpFile.exists()) tmpFile.delete()
  }

  check(dbFile.exists() && dbFile.isFile) { "database file missing after extraction — asset may be corrupt" }
  Timber.i("database initialised: $dbFile (${dbFile.length()} bytes)")
}
