package com.alelk.pws.database.helper

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alelk.pws.database.R
import timber.log.Timber
import java.io.*

/**
 * Pws Database Helper
 *
 * Created by Alex Elkin on 29.04.2015.
 */
@Deprecated("use room migrations")
class PwsDatabaseHelper(private val mContext: Context) : SQLiteOpenHelper(
  mContext, DATABASE_NAME, null, DATABASE_VERSION
) {
  private val dbPath: File = mContext.getDatabasePath(DATABASE_NAME)
  private var mNotificationBuilder: NotificationCompat.Builder? = null
  private var mNotificationManager: NotificationManager? = null

  init {
    if (!isDatabaseExists) {
      Log.i(
        LOG_TAG,
        "PwsDatabaseHelper: The current version of database does not exists. Looks like it is " +
          "first app starting. Trying to create database.."
      )
      try {
        mNotificationManager =
          mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationBuilder = NotificationCompat.Builder(mContext, "System")
        mNotificationBuilder!!.setContentTitle(mContext.getString(R.string.txt_title_database_init))
          .setSmallIcon(R.drawable.ic_data_usage_black)
          .setTicker(mContext.getString(R.string.txt_title_database_init))
        copyDatabase()
        applyMigrations()
        if (!isDatabaseExists) {
          Log.e(
            LOG_TAG,
            "PwsDatabaseHelper: Database was not be copied from asset folder: Database does not exists: $dbPath"
          )
        } else {
          mergePreviousDatabase()
          removePreviousDatabaseIfExists()
        }
      } catch (e: IOException) {
        Log.e(
          LOG_TAG,
          "PwsDatabaseHelper: Error copying database file: " + e.localizedMessage
        )
      } finally {
        mNotificationManager?.cancel(
          DB_INIT_NOTIFICATION_ID
        )
      }
    }
  }

  override fun onOpen(db: SQLiteDatabase) {
    Log.v(LOG_TAG, "${this::onOpen.name}: PWS database opened '$DATABASE_NAME' version $DATABASE_VERSION")
  }

  override fun onCreate(db: SQLiteDatabase) {
    Log.e(LOG_TAG, "${this::onCreate.name}: This method should be never called.")
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    Log.i(LOG_TAG, "${this::onUpgrade.name}: oldVersion: $oldVersion. newVersion: $newVersion")
    for (version in oldVersion + 1..newVersion) {
      val scriptPath = "db/migrations/${version}.sql"
      executeScript(scriptPath, db, this::onUpgrade.name)
    }
  }


  override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    Log.i(LOG_TAG, "${this::onDowngrade.name}: oldVersion: $oldVersion. newVersion: $newVersion")
    for (version in oldVersion downTo newVersion + 1) {
      val scriptPath = "db/rollbacks/${version}.sql"
      executeScript(scriptPath, db, this::onDowngrade.name)
    }
  }

  private fun executeScript(
    scriptPath: String,
    db: SQLiteDatabase,
    logMethodName: String
  ) {
    val sqlScript = readSqlScriptFromFile(scriptPath)
    if (sqlScript.isNotEmpty()) {
      db.beginTransaction()
      try {
        val commands = sqlScript.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        commands.forEach { command ->
          db.execSQL(command)
        }
        db.setTransactionSuccessful()
      } catch (e: SQLException) {
        Log.e(LOG_TAG, "$logMethodName: Error while performing $scriptPath", e)
      } finally {
        db.endTransaction()
        Log.i(LOG_TAG, "$logMethodName: $scriptPath executed successfully")
      }
    }
  }

  private fun readSqlScriptFromFile(fileName: String): String {
    val stringBuilder = StringBuilder()
    try {
      mContext.assets.open(fileName).use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
          var line: String?
          while (reader.readLine().also { line = it } != null) {
            val trimmedLine = line!!.trim()
            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("--")) {
              stringBuilder.append(line).append("\n")
            }
          }
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return stringBuilder.toString()
  }

  @Throws(IOException::class)
  private fun copyDatabase() = kotlin.runCatching {
    val am = mContext.assets
    val buffer = ByteArray(1024)
    val dbFolder = dbPath.parentFile!!
    if (!dbFolder.exists() || !dbFolder.isDirectory) {
      if (!dbFolder.mkdirs()) {
        Log.e(LOG_TAG, "could not create directory $dbFolder")
      }
    }
    val fileList = am.list(ASSETS_DB_FOLDER)
    if (fileList.isNullOrEmpty()) {
      Log.e(LOG_TAG, "no database files found in asset directory $ASSETS_DB_FOLDER")
      return@runCatching
    }
    val zipFile = dbPath.parentFile!!.resolve(dbPath.name + "z")
    FileOutputStream(zipFile).use { zipOutputStream ->
      for (i in 1..fileList.size) {
        publishProgress(R.string.txt_copy_files, fileList.size, i)
        am.open("$ASSETS_DB_FOLDER/${DATABASE_NAME}z.$i").use { inputStream ->
          var count: Int
          while (inputStream.read(buffer).also { count = it } != -1) {
            zipOutputStream.write(buffer, 0, count)
          }
          Log.i(LOG_TAG, "copying success: file $ASSETS_DB_FOLDER/$DATABASE_NAME.$i")
        }
      }
    }
    zipFile.unzip(dbFolder)
    if (!dbPath.exists() || !dbPath.isFile) {
      Log.e(LOG_TAG, "database file from asset has invalid name")
    }
  }.onFailure { e ->
    Log.e(LOG_TAG, "error copying database file from assets: ${e.message}", e)
  }

  private fun applyMigrations() {
    val METHOD_NAME = "applyMigrations"
    val database = openDatabase(dbPath, SQLiteDatabase.OPEN_READWRITE)
    if (database == null) {
      Log.e(LOG_TAG, "$METHOD_NAME: Could not open database + $dbPath")
      return
    }

    try {
      val migrationFiles = mContext.assets.list("db/migrations")?.filter { fileName ->
        val versionNumber = fileName.replace(".sql", "").toIntOrNull()
        versionNumber != null && versionNumber <= DATABASE_VERSION
      }
      migrationFiles?.sorted()?.forEach { fileName ->
        val scriptPath = "db/migrations/$fileName"
        executeScript(scriptPath, database, METHOD_NAME)
      }
    } catch (e: IOException) {
      Log.e(LOG_TAG, "$METHOD_NAME: Error applying migration files", e)
    } finally {
      database.close()
    }
  }

  private fun openPreviousDatabaseIfExists(): SQLiteDatabase? {
    for (dbName in DATABASE_PREVIOUS_NAMES) {
      val databasePath = mContext.getDatabasePath(dbName)
      val database = openDatabase(databasePath, SQLiteDatabase.OPEN_READONLY)
      if (database != null) return database
    }
    return null
  }

  private fun removePreviousDatabaseIfExists() {
    for (dbName in DATABASE_PREVIOUS_NAMES) {
      val databasePath = mContext.getDatabasePath(dbName).path
      val file = File(databasePath)
      if (file.exists() && file.isFile) {
        Log.i(
          LOG_TAG,
          "removePreviousDatabaseIfExists: Previous version of database will be removed: "
            + databasePath
        )
        file.delete()
        return
      }
    }
  }

  private fun mergePreviousDatabase() {
    try {
      openPreviousDatabaseIfExists()?.use { prevDatabase ->
        openDatabase(dbPath, SQLiteDatabase.OPEN_READWRITE)?.use { database ->
          Log.i(
            LOG_TAG, this::mergePreviousDatabase.name +
              "Merge user data from database ${prevDatabase.path} version ${prevDatabase.version}) to ${database.path} (version ${database.version})"
          )
          fun mergeFavorites() = kotlin.runCatching {
            queryFavorites091(prevDatabase).use { cursor ->
              Log.v(LOG_TAG, "${this::mergePreviousDatabase.name}: Count of favorites will be merged from previous database: ${cursor.count}")
              insertFavorites(database, cursor)
            }
          }.onFailure { exc -> Log.e(LOG_TAG, "unable to merge favorites: ${exc.message}", exc) }

          fun mergeHistory() = kotlin.runCatching {
            queryHistory091(prevDatabase).use { cursor ->
              Log.v(LOG_TAG, "${this::mergePreviousDatabase.name}: Count of history items will be merged from previous database: ${cursor.count}")
              insertHistory(database, cursor)
            }
          }.onFailure { exc -> Log.e(LOG_TAG, "unable to merge history: ${exc.message}", exc) }

          fun mergeEditedSongs() = kotlin.runCatching {
            prevDatabase.queryEditedSongs120().use { cursor ->
              Log.v(LOG_TAG, "${this::mergePreviousDatabase.name}: Count of edited songs will be merged from previous database: ${cursor.count}")
              insertEditedSongs(database, cursor)
            }
          }.onFailure { exc -> Log.e(LOG_TAG, "unable to merge edited songs: ${exc.message}", exc) }

          when (prevDatabase.version) {
            DATABASE_VERSION_091, DATABASE_VERSION_110, DATABASE_VERSION_120 -> {
              mergeFavorites()
              mergeHistory()
            }

            4 -> {
              mergeFavorites()
              mergeHistory()
              mergeEditedSongs()
            }

            else -> Log.e(LOG_TAG, "${this::mergePreviousDatabase.name}: Unexpected database version: ${prevDatabase.version}")
          }
        }.also { if (it == null) Log.e(LOG_TAG, "${this::mergePreviousDatabase.name}: Could not open database + $dbPath") }
      }
    } catch (e: Throwable) {
      Log.e(LOG_TAG, "${this::mergePreviousDatabase.name}: Error merging previous database: ${e.message}", e)
    }
  }

  private fun queryFavorites091(db: SQLiteDatabase): Cursor = db.query(
    false,
    "favorites as f inner join psalmnumbers as pn on f.psalmnumberid=pn._id inner join books as b on pn.bookid=b._id",
    arrayOf("f.position as position", "pn.number as number", "b.edition as edition"),
    null, null, null, null, null, null
  )

  private fun queryHistory091(db: SQLiteDatabase): Cursor = db.query(
    false,
    "history as h inner join psalmnumbers as pn on h.psalmnumberid=pn._id inner join books as b on pn.bookid=b._id",
    arrayOf("h.accesstimestamp as accesstimestamp", "pn.number as number", "b.edition as edition"),
    null, null, null, null, null, null
  )

  private fun SQLiteDatabase.queryEditedSongs120(): Cursor =
    this.query(
      false,
      "psalms as p inner join psalmnumbers as pn on pn.psalmid=p._id inner join books b on pn.bookid=b._id",
      arrayOf("p.text as text", "p.bibleref as bibleref", "p.tonalities as tonalities", "pn.number as number", "b.edition as edition"),
      "p.edited=?", arrayOf(1.toString()), null, null, null, null
    )

  @SuppressLint("Range")
  private fun insertEditedSongs(db: SQLiteDatabase, cursor: Cursor) {
    if (!cursor.moveToFirst()) {
      Log.d(LOG_TAG, "${this::insertEditedSongs.name}: unable insert edited songs: no edited songs found in previous db")
      return
    }
    do {
      val text = cursor.getString(cursor.getColumnIndex("text"))
      val bibleRef = cursor.getString(cursor.getColumnIndex("bibleref"))
      val tonalities = cursor.getString(cursor.getColumnIndex("tonalities"))
      val number = cursor.getInt(cursor.getColumnIndex("number"))
      val edition = cursor.getString(cursor.getColumnIndex("edition"))
      db
        .query(
          false, "psalms as p inner join psalmnumbers as pn on pn.psalmid=p._id inner join books b on pn.bookid=b._id",
          arrayOf("p._id as _id"),
          "b.edition= '$edition' and pn.number=$number",
          null, null, null, null, null
        )
        .use { current ->
          if (!current.moveToFirst()) {
            Log.w(LOG_TAG, "${this::insertEditedSongs.name}: cannot find psalm with number=$number and edition=$edition in current database.")
          } else {
            val contentValues = ContentValues()
            contentValues.put("text", text)
            contentValues.put("bibleref", bibleRef)
            contentValues.put("tonalities", tonalities)
            val psalmId = current.getLong(current.getColumnIndex("_id"))
            db.update("psalms", contentValues, "_id=?", arrayOf(psalmId.toString()))
            Log.v(LOG_TAG, "${this::insertEditedSongs.name}: Inserted new item to songs: edition=$edition number=$number")
          }
        }
    } while (cursor.moveToNext())
  }

  @SuppressLint("Range")
  private fun insertFavorites(db: SQLiteDatabase, cursor: Cursor) {
    if (!cursor.moveToFirst()) {
      Log.d(LOG_TAG, "${this::insertFavorites.name}: unable insert favorites: no favorites found in previous db")
      return
    }
    do {
      val edition = cursor.getString(cursor.getColumnIndex("edition"))
      val number = cursor.getInt(cursor.getColumnIndex("number"))
      db
        .query(
          false,
          "psalmnumbers AS pn INNER JOIN books as b ON pn.bookid=b._id",
          arrayOf("pn._id as _id", "pn._id as psalmnumberid", "pn.number as psalm_number", "pn.bookid as book_id"),
          "b.edition like '$edition' and pn.number=$number",
          null, null, null, null, null
        )
        .use { current ->
          if (!current.moveToFirst()) {
            Log.w(LOG_TAG, "${this::insertFavorites.name}: cannot find psalm with number=$number and edition=$edition in current database.")
          } else {
            val contentValues = ContentValues()
            contentValues.put("position", cursor.getLong(cursor.getColumnIndex("position")))
            contentValues.put(
              "psalmnumberid",
              current.getLong(current.getColumnIndex("psalmnumberid"))
            )
            db.insert("favorites", null, contentValues)
            Log.v(LOG_TAG, "${this::insertFavorites.name}: Inserted new item to favorites: edition=$edition number=$number")
          }
        }
    } while (cursor.moveToNext())
  }

  @SuppressLint("Range")
  private fun insertHistory(db: SQLiteDatabase, cursor: Cursor) {
    val METHOD_NAME = "insertHistory"
    if (!cursor.moveToFirst()) {
      Timber.d("unable insert history: no history items found in previous db")
      return
    }
    do {
      var current: Cursor? = null
      try {
        val edition = cursor.getString(cursor.getColumnIndex("edition"))
        val number = cursor.getInt(cursor.getColumnIndex("number"))
        current = db.query(
          false,
          "psalmnumbers AS pn INNER JOIN books as b ON pn.bookid=b._id",
          arrayOf("pn._id as _id", "pn._id as psalmnumberid", "pn.number as psalm_number", "pn.bookid as book_id"),
          "b.edition like '$edition' and pn.number=$number",
          null, null, null, null, null
        )
        if (!current.moveToFirst()) {
          Log.w(
            LOG_TAG, METHOD_NAME + ": cannot find psalm with number=" + number +
              " and edition=" + edition + " in current database."
          )
          continue
        }
        val contentValues = ContentValues()
        contentValues.put("accesstimestamp", cursor.getString(cursor.getColumnIndex("accesstimestamp")))
        contentValues.put("psalmnumberid", current.getLong(current.getColumnIndex("psalmnumberid")))
        db.insert("history", null, contentValues)
        Log.v(LOG_TAG, METHOD_NAME + ": Inserted new item to history: edition=" + edition + " number=" + number)
      } finally {
        current?.close()
      }
    } while (cursor.moveToNext())
  }

  private val isDatabaseExists: Boolean get() = dbPath.exists() && dbPath.isFile

  private fun openDatabase(dbPath: File, flags: Int): SQLiteDatabase? {
    try {
      return SQLiteDatabase.openDatabase(dbPath.toString(), null, flags)
    } catch (ex: SQLiteException) {
      Log.i(
        LOG_TAG,
        "openDatabase: Database " + dbPath + " does not exists. Message: " + ex.localizedMessage
      )
    }
    return null
  }

  private fun publishProgress(resourceId: Int, max: Int, current: Int) {
    if (mNotificationBuilder == null || mNotificationManager == null) {
      Log.w(LOG_TAG, "publishProgress: cannot show notification")
      return
    }
    mNotificationBuilder?.setContentText(
      String.format(
        mContext.getString(resourceId),
        current,
        max
      )
    )
    mNotificationBuilder?.setProgress(max, current, false)
    mNotificationManager?.notify(DB_INIT_NOTIFICATION_ID, mNotificationBuilder?.build())
  }

  companion object {
    const val DATABASE_VERSION = 10
    const val DATABASE_NAME = "pws.1.8.0.db"
    private val DATABASE_PREVIOUS_NAMES = arrayOf("pws.1.2.0.db", "pws.1.1.0.db", "pws.0.9.1.db")
    private const val DATABASE_VERSION_091 = 1
    private const val DATABASE_VERSION_110 = 2
    private const val DATABASE_VERSION_120 = 3
    private const val DB_INIT_NOTIFICATION_ID = 1331
    private val LOG_TAG = PwsDatabaseHelper::class.java.simpleName
    private const val ASSETS_DB_FOLDER = "db"
  }
}