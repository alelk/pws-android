package io.github.alelk.pws.database.helper

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
import io.github.alelk.pws.database.R
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
        if (!isDatabaseExists) {
          Log.e(
            LOG_TAG,
            "PwsDatabaseHelper: Database was not be copied from asset folder: Database does not exists: $dbPath"
          )
        } else {
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

  override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
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

  private fun SQLiteDatabase.queryEditedSongs120(): Cursor =
    this.query(
      false,
      "psalms as p inner join psalmnumbers as pn on pn.psalmid=p._id inner join books b on pn.bookid=b._id",
      arrayOf("p.text as text", "p.bibleref as bibleref", "p.tonalities as tonalities", "pn.number as number", "b.edition as edition"),
      "p.edited=?", arrayOf(1.toString()), null, null, null, null
    )


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
    const val DATABASE_NAME = "pws.2.0.0.db"
    private val DATABASE_PREVIOUS_NAMES = arrayOf("pws.1.8.0.db", "pws.1.2.0.db", "pws.1.1.0.db", "pws.0.9.1.db")
    private const val DATABASE_VERSION_091 = 1
    private const val DATABASE_VERSION_110 = 2
    private const val DATABASE_VERSION_120 = 3
    private const val DB_INIT_NOTIFICATION_ID = 1331
    private val LOG_TAG = PwsDatabaseHelper::class.java.simpleName
    private const val ASSETS_DB_FOLDER = "db"
  }
}