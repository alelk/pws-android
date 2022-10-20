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
package com.alelk.pws.database.table

import com.alelk.pws.database.table.PwsTableHelper.Companion.isTriggerExists
import com.alelk.pws.database.table.PwsTableHelper.Companion.isTableExists
import android.os.Build
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import android.database.Cursor
import android.text.TextUtils
import android.util.Log
import com.alelk.pws.database.table.PwsPsalmTable.COLUMN_ANNOTATION
import com.alelk.pws.database.table.PwsPsalmTable.COLUMN_AUTHOR
import com.alelk.pws.database.table.PwsPsalmTable.COLUMN_COMPOSER
import com.alelk.pws.database.table.PwsPsalmTable.COLUMN_ID
import com.alelk.pws.database.table.PwsPsalmTable.COLUMN_NAME
import com.alelk.pws.database.table.PwsPsalmTable.COLUMN_TEXT
import com.alelk.pws.database.table.PwsPsalmTable.COLUMN_TRANSLATOR
import com.alelk.pws.database.table.PwsPsalmTable.TABLE_PSALMS
import java.util.*

/**
 * Pws Psalm FTS Table
 * Implements functionality to providing Full Text Search functionality for psalm.
 *
 * Created by Alex Elkin on 15.03.2016.
 */
object PwsPsalmFtsTable : PwsTable {
  private val LOG_TAG = PwsPsalmFtsTable::class.java.simpleName
  const val TABLE_PSALMS_FTS = "psalms_fts"
  private const val TRIGGER_BEFORE_UPDATE = TABLE_PSALMS_FTS + "_bu"
  private const val TRIGGER_BEFORE_DELETE = TABLE_PSALMS_FTS + "_bd"
  private const val TRIGGER_AFTER_UPDATE = TABLE_PSALMS_FTS + "_au"
  private const val TRIGGER_AFTER_INSERT = TABLE_PSALMS_FTS + "_ai"
  private const val TOKENIZER_API21 = "icu ru_RU"
  private val IS_API_21 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
  private val TABLE_CREATE_SCRIPT =
    "create virtual table $TABLE_PSALMS_FTS using fts4 (content=$TABLE_PSALMS, $COLUMN_NAME, $COLUMN_AUTHOR, $COLUMN_TRANSLATOR, $COLUMN_COMPOSER, $COLUMN_ANNOTATION, $COLUMN_TEXT${if (IS_API_21) ", tokenize=" + TOKENIZER_API21 else ""});"
  private const val TABLE_DROP_SCRIPT = "drop table if exists $TABLE_PSALMS_FTS"
  private const val TABLE_FILL_VALUES_SCRIPT =
    "insert into $TABLE_PSALMS_FTS(docid, $COLUMN_NAME, $COLUMN_AUTHOR, $COLUMN_TRANSLATOR, $COLUMN_COMPOSER, $COLUMN_ANNOTATION, $COLUMN_TEXT) select $COLUMN_ID, $COLUMN_NAME, $COLUMN_AUTHOR, $COLUMN_TRANSLATOR, $COLUMN_COMPOSER, $COLUMN_ANNOTATION, $COLUMN_TEXT from $TABLE_PSALMS;"
  private val TABLE_PSALMS_PROJECTION = arrayOf(
    COLUMN_ID,
    COLUMN_NAME,
    COLUMN_AUTHOR,
    COLUMN_TRANSLATOR,
    COLUMN_COMPOSER,
    COLUMN_ANNOTATION,
    COLUMN_TEXT
  )
  private const val TRIGGER_BU_SCRIPT =
    "create trigger $TRIGGER_BEFORE_UPDATE before update on $TABLE_PSALMS begin delete from $TABLE_PSALMS_FTS where docid=old.rowid; end;"
  private const val TRIGGER_BD_SCRIPT =
    "create trigger $TRIGGER_BEFORE_DELETE before delete on $TABLE_PSALMS begin delete from $TABLE_PSALMS_FTS where docid=old.rowid; end;"
  private const val TRIGGER_AU_SCRIPT =
    "create trigger $TRIGGER_AFTER_UPDATE after update on $TABLE_PSALMS begin insert into $TABLE_PSALMS_FTS(docid, $COLUMN_NAME, $COLUMN_AUTHOR, $COLUMN_TRANSLATOR, $COLUMN_COMPOSER, $COLUMN_ANNOTATION, $COLUMN_TEXT) values(new.rowid, new.$COLUMN_NAME, new.$COLUMN_AUTHOR, new.$COLUMN_TRANSLATOR, new.$COLUMN_COMPOSER, new.$COLUMN_ANNOTATION, new.$COLUMN_TEXT); end;"
  private const val TRIGGER_AI_SCRIPT =
    "create trigger $TRIGGER_AFTER_INSERT after insert on $TABLE_PSALMS begin insert into $TABLE_PSALMS_FTS(docid, $COLUMN_NAME, $COLUMN_AUTHOR, $COLUMN_TRANSLATOR, $COLUMN_COMPOSER, $COLUMN_ANNOTATION, $COLUMN_TEXT) values(new.rowid, new.$COLUMN_NAME, new.$COLUMN_AUTHOR, new.$COLUMN_TRANSLATOR, new.$COLUMN_COMPOSER, new.$COLUMN_ANNOTATION, new.$COLUMN_TEXT); end;"
  private const val TRIGGER_BU_DROP_SCRIPT = "drop trigger if exists $TRIGGER_BEFORE_UPDATE"
  private const val TRIGGER_BD_DROP_SCRIPT = "drop trigger if exists $TRIGGER_BEFORE_DELETE"
  private const val TRIGGER_AU_DROP_SCRIPT = "drop trigger if exists $TRIGGER_AFTER_UPDATE"
  private const val TRIGGER_AI_DROP_SCRIPT = "drop trigger if exists $TRIGGER_AFTER_INSERT"
  fun createTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_CREATE_SCRIPT)
  }

  fun populateTable(db: SQLiteDatabase, progressListener: (max: Int, current: Int) -> Unit) {
    val METHOD_NAME = "populateTable"
    if (IS_API_21) {
      db.execSQL(TABLE_FILL_VALUES_SCRIPT)
      return
    }
    var cursor = db.query(
      TABLE_PSALMS,
      arrayOf("max($COLUMN_ID) as maxid"),
      null,
      null,
      null,
      null,
      null
    )
    if (cursor == null || !cursor.moveToFirst()) {
      Log.w(
        LOG_TAG,
        "$METHOD_NAME: looks like no rows in '$TABLE_PSALMS' table. Could not populate '$TABLE_PSALMS_FTS' table"
      )
      return
    }
    val maxId = cursor.getLong(cursor.getColumnIndex("maxid"))
    var id: Long = 0
    while (id < maxId) {
      progressListener(maxId.toInt(), id.toInt())
      Log.v(
        LOG_TAG,
        "$METHOD_NAME: Insert to $TABLE_PSALMS_FTS table rows with id between $id and ${id + 100} from $TABLE_PSALMS table."
      )
      cursor = query(db, id, id + 99)
      if (cursor == null || !cursor.moveToFirst()) {
        Log.w(
          LOG_TAG,
          "$METHOD_NAME: unsuccessful attempt to query data from '$TABLE_PSALMS' table with id between $id and ${id + 100}. No results."
        )
        id += 100
        continue
      }
      do {
        db.insert(TABLE_PSALMS_FTS, null, fromPsalmsTableCursor(cursor))
      } while (cursor.moveToNext())
      id += 100
    }
  }

  private fun query(db: SQLiteDatabase, fromId: Long, toId: Long): Cursor {
    return db.query(
      TABLE_PSALMS,
      TABLE_PSALMS_PROJECTION,
      "_id >= $fromId and _id <= $toId",
      null,
      null,
      null,
      null
    )
  }

  private fun fromPsalmsTableCursor(cursor: Cursor): ContentValues {
    val contentValues = ContentValues()
    contentValues.put("docid", cursor.getLong(cursor.getColumnIndex(COLUMN_ID)))
    contentValues.put(
      COLUMN_NAME, cursor.getString(cursor.getColumnIndex(COLUMN_NAME)).lowercase(
        Locale.getDefault()
      )
    )
    val translator = cursor.getString(cursor.getColumnIndex(COLUMN_TRANSLATOR))
    if (!TextUtils.isEmpty(translator)) {
      contentValues.put(COLUMN_TRANSLATOR, translator.lowercase(Locale.getDefault()))
    }
    val composer = cursor.getString(cursor.getColumnIndex(COLUMN_COMPOSER))
    if (!TextUtils.isEmpty(composer)) {
      contentValues.put(COLUMN_COMPOSER, composer.lowercase(Locale.getDefault()))
    }
    val author = cursor.getString(cursor.getColumnIndex(COLUMN_AUTHOR))
    if (!TextUtils.isEmpty(author)) {
      contentValues.put(COLUMN_AUTHOR, author.lowercase(Locale.getDefault()))
    }
    val annotation = cursor.getString(cursor.getColumnIndex(COLUMN_ANNOTATION))
    if (!TextUtils.isEmpty(annotation)) {
      contentValues.put(COLUMN_AUTHOR, annotation.lowercase(Locale.getDefault()))
    }
    contentValues.put(
      COLUMN_TEXT, cursor.getString(cursor.getColumnIndex(COLUMN_TEXT)).lowercase(
        Locale.getDefault()
      )
    )
    return contentValues
  }

  fun dropTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_DROP_SCRIPT)
  }

  fun setUpAllTriggers(db: SQLiteDatabase) {
    setBeforeUpdateTrigger(db)
    setBeforeDeleteTrigger(db)
    setAfterUpdateTrigger(db)
    setAfterInsertTrigger(db)
  }

  fun dropAllTriggers(db: SQLiteDatabase) {
    dropBeforeUpdateTrigger(db)
    dropBeforeDeleteTrigger(db)
    dropAfterUpdateTrigger(db)
    dropAfterInsertTrigger(db)
  }

  fun isAllTriggersExists(db: SQLiteDatabase): Boolean {
    return isAiTriggerExists(db) && isAuTriggerExists(db) && isBuTriggerExists(db) && isBdTriggerExists(
      db
    )
  }

  private fun setBeforeUpdateTrigger(db: SQLiteDatabase) {
    db.execSQL(TRIGGER_BU_SCRIPT)
  }

  private fun setBeforeDeleteTrigger(db: SQLiteDatabase) {
    db.execSQL(TRIGGER_BD_SCRIPT)
  }

  private fun setAfterUpdateTrigger(db: SQLiteDatabase) {
    db.execSQL(TRIGGER_AU_SCRIPT)
  }

  private fun setAfterInsertTrigger(db: SQLiteDatabase) {
    db.execSQL(TRIGGER_AI_SCRIPT)
  }

  private fun dropBeforeUpdateTrigger(db: SQLiteDatabase) {
    db.execSQL(TRIGGER_BU_DROP_SCRIPT)
  }

  private fun dropBeforeDeleteTrigger(db: SQLiteDatabase) {
    db.execSQL(TRIGGER_BD_DROP_SCRIPT)
  }

  private fun dropAfterUpdateTrigger(db: SQLiteDatabase) {
    db.execSQL(TRIGGER_AU_DROP_SCRIPT)
  }

  private fun dropAfterInsertTrigger(db: SQLiteDatabase) {
    db.execSQL(TRIGGER_AI_DROP_SCRIPT)
  }

  private fun isBuTriggerExists(db: SQLiteDatabase): Boolean {
    return isTriggerExists(db, TRIGGER_BEFORE_UPDATE)
  }

  private fun isBdTriggerExists(db: SQLiteDatabase): Boolean {
    return isTriggerExists(db, TRIGGER_BEFORE_DELETE)
  }

  private fun isAuTriggerExists(db: SQLiteDatabase): Boolean {
    return isTriggerExists(db, TRIGGER_AFTER_UPDATE)
  }

  private fun isAiTriggerExists(db: SQLiteDatabase): Boolean {
    return isTriggerExists(db, TRIGGER_AFTER_INSERT)
  }

  fun isTableExists(db: SQLiteDatabase): Boolean {
    return isTableExists(db, TABLE_PSALMS_FTS)
  }
}