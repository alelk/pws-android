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
 * Pws Psalm Number Table
 *
 * Created by Alex Elkin on 22.04.2015.
 */
object PwsPsalmNumbersTable : PwsTable {
  const val TABLE_PSALMNUMBERS = "psalmnumbers"
  const val COLUMN_ID = "_id"
  const val COLUMN_PSALMID = "psalmid"
  const val COLUMN_BOOKID = "bookid"
  const val COLUMN_NUMBER = "number"

  private const val TABLE_CREATE_SCRIPT =
    "create table $TABLE_PSALMNUMBERS($COLUMN_ID integer primary key autoincrement, $COLUMN_NUMBER integer not null, $COLUMN_PSALMID integer not null, $COLUMN_BOOKID integer not null, FOREIGN KEY ($COLUMN_PSALMID) REFERENCES ${PwsPsalmTable.TABLE_PSALMS} (${PwsPsalmTable.COLUMN_ID}), FOREIGN KEY ($COLUMN_BOOKID) REFERENCES ${PwsBookTable.TABLE_BOOKS} (${PwsBookTable.COLUMN_ID}));"

  private const val TABLE_DROP_SCRIPT = "drop table if exists $TABLE_PSALMNUMBERS"

  fun createTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_CREATE_SCRIPT)
  }

  fun dropTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_DROP_SCRIPT)
  }
}