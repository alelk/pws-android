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
 * PWS History Table
 *
 * Created by Alex Elkin on 19.02.2016.
 */
object PwsHistoryTable {
  const val TABLE_HISTORY = "history"
  const val COLUMN_ID = "_id"
  const val COLUMN_PSALMNUMBERID = "psalmnumberid"
  const val COLUMN_ACCESSTIMESTAMP = "accesstimestamp"

  private const val TABLE_CREATE_SCRIPT =
    "create table $TABLE_HISTORY($COLUMN_ID integer primary key autoincrement, $COLUMN_PSALMNUMBERID integer not null, $COLUMN_ACCESSTIMESTAMP string not null, FOREIGN KEY ($COLUMN_PSALMNUMBERID) REFERENCES ${PwsPsalmNumbersTable.TABLE_PSALMNUMBERS} (${PwsPsalmNumbersTable.COLUMN_ID}));"

  private const val TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_HISTORY

  fun createTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_CREATE_SCRIPT)
  }

  fun dropTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_DROP_SCRIPT)
  }
}