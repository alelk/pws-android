/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alelk.pws.database.helper

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
import com.alelk.pws.database.provider.PwsDataProviderContract
import com.alelk.pws.database.table.*
import java.io.*

/**
 * Pws Database Helper
 *
 * Created by Alex Elkin on 29.04.2015.
 */
class PwsDatabaseHelper(private val mContext: Context) : SQLiteOpenHelper(
  mContext, DATABASE_NAME, null, DATABASE_VERSION
) {
  private val dbFolder: String
  private val dbPath: String
  private var mNotificationBuilder: NotificationCompat.Builder? = null
  private var mNotificationManager: NotificationManager? = null

  init {
    dbPath = mContext.getDatabasePath(DATABASE_NAME).path
    dbFolder = mContext.getDatabasePath(DATABASE_NAME).parent + "/"
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
          setUpPsalmFts()
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
    val METHOD_NAME = "onOpen"
    Log.v(
      LOG_TAG,
      "$METHOD_NAME: PWS database opened '$DATABASE_NAME' version $DATABASE_VERSION"
    )
  }

  override fun onCreate(db: SQLiteDatabase) {
    val METHOD_NAME = "onCreate"
    Log.e(LOG_TAG, "$METHOD_NAME: This method should be never called.")
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    val METHOD_NAME = "onUpgrade"
    Log.i(LOG_TAG, "$METHOD_NAME: oldVersion: $oldVersion. newVersion: $newVersion")
    for (version in oldVersion + 1..newVersion) {
      val scriptPath = "db/migrations/${version}.sql"
      executeScript(scriptPath, db, METHOD_NAME)
    }
  }


  override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    val METHOD_NAME = "onDowngrade"
    Log.i(LOG_TAG, "$METHOD_NAME: oldVersion: $oldVersion. newVersion: $newVersion")
    for (version in oldVersion downTo newVersion + 1) {
      val scriptPath = "db/rollbacks/${version}.sql"
      executeScript(scriptPath, db, METHOD_NAME)
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
  private fun copyDatabase() {
    val METHOD_NAME = "copyDatabase"
    val am = mContext.assets
    var outputStream: OutputStream? = null
    var inputStream: InputStream? = null
    val buffer = ByteArray(1024)
    try {
      val dbFolder = File(dbFolder)
      if (!dbFolder.exists() || !dbFolder.isDirectory) {
        if (!dbFolder.mkdir()) {
          Log.e(LOG_TAG, METHOD_NAME + ": Could not create directory: " + this.dbFolder)
        }
      }
      val fileList = am.list(ASSETS_DB_FOLDER)
      if (fileList == null || fileList.isEmpty()) {
        Log.e(
          LOG_TAG,
          "$METHOD_NAME: No database files found in asset directory $ASSETS_DB_FOLDER"
        )
        return
      }
      outputStream = FileOutputStream(dbPath)
      for (i in 1..fileList.size) {
        publishProgress(R.string.txt_copy_files, fileList.size, i)
        try {
          inputStream = am.open("$ASSETS_DB_FOLDER/$DATABASE_NAME.$i")
          var count: Int
          while (inputStream.read(buffer).also { count = it } != -1) {
            outputStream.write(buffer, 0, count)
          }
          Log.i(
            LOG_TAG,
            "$METHOD_NAME: Copying success: File $ASSETS_DB_FOLDER/$DATABASE_NAME.$i"
          )
        } catch (ex: FileNotFoundException) {
          if (i == 1) {
            Log.w(
              LOG_TAG,
              METHOD_NAME + ": Could not open asset database file: " + ex.localizedMessage
            )
          }
          return
        } finally {
          try {
            inputStream?.close()
          } catch (e: IOException) {
            Log.e(
              LOG_TAG,
              METHOD_NAME + ": Error closing input stream: " + e.localizedMessage
            )
          }
        }
      }
    } catch (ex: FileNotFoundException) {
      Log.w(LOG_TAG, METHOD_NAME + ": Error copying database file: " + ex.localizedMessage)
    } finally {
      if (outputStream != null) {
        try {
          outputStream.flush()
          outputStream.close()
        } catch (e: IOException) {
          Log.e(
            LOG_TAG,
            METHOD_NAME + ": Error closing output stream: " + e.localizedMessage
          )
        }
      }
    }
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
      val databasePath = mContext.getDatabasePath(dbName).path
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
    val METHOD_NAME = "mergePreviousDatabase"
    val prevDatabase = openPreviousDatabaseIfExists() ?: return
    val database = openDatabase(dbPath, SQLiteDatabase.OPEN_READWRITE)
    if (database == null) {
      Log.e(LOG_TAG, "$METHOD_NAME: Could not open database + $dbPath")
      return
    }
    Log.i(
      LOG_TAG, METHOD_NAME + ": Merge user data from database " +
        prevDatabase.path + " (version " + prevDatabase.version + ") to " +
        database.path + " (version " + database.version + ")"
    )
    try {
      when (prevDatabase.version) {
        DATABASE_VERSION_091, DATABASE_VERSION_110 -> {
          var cursor = queryFavorites091(prevDatabase)
          Log.v(
            LOG_TAG,
            "$METHOD_NAME: Count of favorites will be merged from previous database: ${cursor.count}"
          )
          insertFavorites(database, cursor)
          cursor.close()
          cursor = queryHistory091(prevDatabase)
          Log.v(
            LOG_TAG,
            "$METHOD_NAME: Count of history items will be merged from previous database: ${cursor.count}"
          )
          insertHistory(database, cursor)
          cursor.close()
        }
        else -> Log.e(
          LOG_TAG,
          "$METHOD_NAME: Unexpected database version: ${prevDatabase.version}"
        )
      }
    } finally {
      prevDatabase.close()
      database.close()
    }
  }

  private fun queryFavorites091(db: SQLiteDatabase): Cursor {
    return db.query(
      false,
      "favorites as f inner join psalmnumbers as pn on f.psalmnumberid=pn._id inner join books as b on pn.bookid=b._id",
      arrayOf("f.position as position", "pn.number as number", "b.edition as edition"),
      null,
      null,
      null,
      null,
      null,
      null
    )
  }

  private fun queryHistory091(db: SQLiteDatabase): Cursor {
    return db.query(
      false,
      "history as h inner join psalmnumbers as pn on h.psalmnumberid=pn._id" +
        " inner join books as b on pn.bookid=b._id",
      arrayOf(
        "h.accesstimestamp as accesstimestamp",
        "pn.number as number",
        "b.edition as edition"
      ),
      null,
      null,
      null,
      null,
      null,
      null
    )
  }

  private fun insertFavorites(db: SQLiteDatabase, cursor: Cursor) {
    val METHOD_NAME = "insertFavorites"
    if (!cursor.moveToFirst()) {
      Log.d(
        LOG_TAG,
        "$METHOD_NAME: unable insert favorites: no favorites found in previous db"
      )
      return
    }
    do {
      var current: Cursor? = null
      try {
        val edition = cursor.getString(cursor.getColumnIndex("edition"))
        val number = cursor.getInt(cursor.getColumnIndex("number"))
        current = db.query(
          false, PwsDataProviderContract.TABLE_PSALMNUMBERS_JOIN_BOOKS,
          PwsDataProviderContract.PsalmNumbers.PROJECTION,
          "b." + PwsBookTable.COLUMN_EDITION + " like '" + edition +
            "' and pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + "=" + number,
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
        contentValues.put(
          PwsFavoritesTable.COLUMN_POSITION,
          cursor.getLong(cursor.getColumnIndex("position"))
        )
        contentValues.put(
          PwsFavoritesTable.COLUMN_PSALMNUMBERID,
          current.getLong(current.getColumnIndex(PwsDataProviderContract.PsalmNumbers.COLUMN_PSALMNUMBER_ID))
        )
        Log.v(
          LOG_TAG, METHOD_NAME + ": Inserted new item to favorites: edition=" + edition
            + " number=" + number
        )
      } finally {
        current?.close()
      }
    } while (cursor.moveToNext())
  }

  private fun insertHistory(db: SQLiteDatabase, cursor: Cursor) {
    val METHOD_NAME = "insertHistory"
    if (!cursor.moveToFirst()) {
      Log.d(
        LOG_TAG,
        "$METHOD_NAME: unable insert history: no history items found in previous db"
      )
      return
    }
    do {
      var current: Cursor? = null
      try {
        val edition = cursor.getString(cursor.getColumnIndex("edition"))
        val number = cursor.getInt(cursor.getColumnIndex("number"))
        current = db.query(
          false, PwsDataProviderContract.TABLE_PSALMNUMBERS_JOIN_BOOKS,
          PwsDataProviderContract.PsalmNumbers.PROJECTION,
          "b." + PwsBookTable.COLUMN_EDITION + " like '" + edition +
            "' and pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + "=" + number,
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
        contentValues.put(
          PwsHistoryTable.COLUMN_ACCESSTIMESTAMP,
          cursor.getString(cursor.getColumnIndex("accesstimestamp"))
        )
        contentValues.put(
          PwsHistoryTable.COLUMN_PSALMNUMBERID,
          current.getLong(current.getColumnIndex(PwsDataProviderContract.PsalmNumbers.COLUMN_PSALMNUMBER_ID))
        )
        Log.v(
          LOG_TAG, METHOD_NAME + ": Inserted new item to history: edition=" + edition
            + " number=" + number
        )
      } finally {
        current?.close()
      }
    } while (cursor.moveToNext())
  }

  private val isDatabaseExists: Boolean
    private get() {
      val file = File(dbPath)
      return file.exists() && file.isFile
    }

  private fun openDatabase(dbPath: String, flags: Int): SQLiteDatabase? {
    try {
      return SQLiteDatabase.openDatabase(dbPath, null, flags)
    } catch (ex: SQLiteException) {
      Log.i(
        LOG_TAG,
        "openDatabase: Database " + dbPath + " does not exists. Message: " + ex.localizedMessage
      )
    }
    return null
  }

  private fun setUpPsalmFts() {
    val METHOD_NAME = "setUpPsalmFts"
    val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
    if (PwsPsalmFtsTable.isTableExists(db) && PwsPsalmFtsTable.isAllTriggersExists(db)) {
      Log.d(
        LOG_TAG,
        "$METHOD_NAME: The PWS Psalm FTS table and it's triggers are exist. No need to recreate."
      )
      return
    }
    PwsPsalmFtsTable.dropAllTriggers(db)
    PwsPsalmFtsTable.dropTable(db)
    PwsPsalmFtsTable.createTable(db)
    PwsPsalmFtsTable.populateTable(db) { max: Int, current: Int ->
      publishProgress(
        R.string.txt_fts_setup,
        max,
        current
      )
    }
    PwsPsalmFtsTable.setUpAllTriggers(db)
    Log.i(
      LOG_TAG,
      "$METHOD_NAME: The PWS Psalm FTS table has been created and populated. All needed triggers are setting up."
    )
    db.close()
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
    private const val DATABASE_VERSION = 4
    private const val DATABASE_NAME = "pws.1.2.0.db"
    private val DATABASE_PREVIOUS_NAMES = arrayOf("pws.1.1.0.db", "pws.0.9.1.db")
    private const val DATABASE_VERSION_091 = 1
    private const val DATABASE_VERSION_110 = 2
    private const val DB_INIT_NOTIFICATION_ID = 1331
    private val LOG_TAG = PwsDatabaseHelper::class.java.simpleName
    private const val ASSETS_DB_FOLDER = "db"
  }
}