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
  if (!dbFolder.exists()) {
    check(dbFolder.mkdirs() || dbFolder.exists()) { "failed to create database dir $dbFolder" }
  }

  val fileList = checkNotNull(am.list(ASSETS_DB_FOLDER)) { "no files in assets/$ASSETS_DB_FOLDER" }
  // DB_ASSET_VERSION = book-content version from db.version (e.g. 3.2.2).
  // Distinct from PwsDatabaseProvider.DB_VERSION (app-side encrypted DB filename, e.g. 3.3.0).
  val expectedSuffix = if (BuildConfig.DB_ASSET_ENCRYPTED) "-${BuildConfig.DB_ASSET_VERSION}.dbz.enc"
                       else "-${BuildConfig.DB_ASSET_VERSION}.dbz"
  val encAsset = fileList.firstOrNull { it.endsWith(expectedSuffix) }
    ?: error(
      "no asset matching *$expectedSuffix found in assets/$ASSETS_DB_FOLDER " +
        "(found: ${fileList.toList()}). Make sure ./fetch-db.sh was run for this variant."
    )

  Timber.i("found asset: $encAsset (encrypted=${BuildConfig.DB_ASSET_ENCRYPTED})")

  val tmpFile = File(context.cacheDir, "pws_init_tmp.db")

  try {
    val encBytes = am.open("$ASSETS_DB_FOLDER/$encAsset").use { it.readBytes() }

    val gzippedDb = if (BuildConfig.DB_ASSET_ENCRYPTED) {
      Timber.i("decrypting $encAsset (${encBytes.size} bytes) ...")
      BundleCrypto.decrypt(encBytes, BundleCrypto.keyFromHex(DbKeyConfig.keyHex()))
    } else {
      Timber.i("asset is plaintext, skipping decryption ...")
      encBytes
    }

    Timber.i("decompressing gzip → temp file ...")
    val dbBytes = GZIPInputStream(gzippedDb.inputStream()).use { it.readBytes() }
    FileOutputStream(tmpFile).use { it.write(dbBytes) }

    Timber.i("encrypting → $dbFile via sqlcipher_export ...")
    // Open (create) the encrypted destination DB with the SQLCipher key, then ATTACH the plain
    // temp DB with empty KEY '' and copy data with sqlcipher_export(target, source).
    // This direction is the canonical pattern and avoids issues with opening a plain SQLite file
    // through the SQLCipher binding.
    val destDb = SQLCipherDatabase.openOrCreateDatabase(dbFile, passphrase, null, null)
    try {
      destDb.execSQL("ATTACH DATABASE '${tmpFile.absolutePath}' AS plaintext KEY ''")
      // sqlcipher_export() is a function that returns rows, so it must be executed via rawQuery,
      // not execSQL (which only accepts non-result statements).
      destDb.rawQuery("SELECT sqlcipher_export('main', 'plaintext')", null).use { it.moveToFirst() }
      destDb.execSQL("DETACH DATABASE plaintext")
    } finally {
      destDb.close()
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
