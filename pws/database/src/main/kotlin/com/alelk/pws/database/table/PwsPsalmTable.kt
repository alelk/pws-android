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

import android.database.sqlite.SQLiteDatabase

/**
 * Pws Psalm Table
 *
 * Created by Alex Elkin on 21.04.2015.
 */
object PwsPsalmTable : PwsTable {
  const val TABLE_PSALMS = "psalms"
  const val COLUMN_ID = "_id"
  const val COLUMN_VERSION = "version"
  const val COLUMN_LOCALE = "locale"
  const val COLUMN_NAME = "name"
  const val COLUMN_AUTHOR = "author"
  const val COLUMN_TRANSLATOR = "translator"
  const val COLUMN_COMPOSER = "composer"
  const val COLUMN_TONALITIES = "tonalities"
  const val COLUMN_YEAR = "year"
  const val COLUMN_ANNOTATION = "bibleref"
  const val COLUMN_TEXT = "text"

  private const val TABLE_CREATE_SCRIPT =
    "create table $TABLE_PSALMS($COLUMN_ID integer primary key autoincrement, $COLUMN_VERSION text not null, $COLUMN_LOCALE text, $COLUMN_NAME text, $COLUMN_AUTHOR text, $COLUMN_TRANSLATOR text, $COLUMN_COMPOSER text, $COLUMN_TONALITIES text, $COLUMN_YEAR text, $COLUMN_ANNOTATION text, $COLUMN_TEXT text not null);"

  private const val TABLE_DROP_SCRIPT = "drop table if exists $TABLE_PSALMS"

  fun createTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_CREATE_SCRIPT)
  }

  fun dropTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_DROP_SCRIPT)
  }
}