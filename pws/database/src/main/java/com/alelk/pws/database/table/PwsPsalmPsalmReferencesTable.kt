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
 * PWS Psalm Psalm Reference Table
 *
 * Created by alelk on 04.01.2017.
 */
object PwsPsalmPsalmReferencesTable : PwsTable {
  const val TABLE_PSALMPSALMREFERENCES = "psalmpsalmreferences"
  const val COLUMN_ID = "_id"
  const val COLUMN_PSALMID = "psalmid"
  const val COLUMN_REFPSALMID = "refpsalmid"
  const val COLUMN_REASON = "reason"
  const val COLUMN_VOLUME = "volume"

  private const val TABLE_CREATE_SCRIPT =
    "create table $TABLE_PSALMPSALMREFERENCES($COLUMN_ID integer primary key autoincrement, $COLUMN_PSALMID integer not null, $COLUMN_REFPSALMID integer not null, $COLUMN_REASON text not null, $COLUMN_VOLUME integer not null, FOREIGN KEY ($COLUMN_PSALMID) REFERENCES ${PwsPsalmTable.TABLE_PSALMS} (${PwsPsalmTable.COLUMN_ID}), FOREIGN KEY ($COLUMN_REFPSALMID) REFERENCES ${PwsPsalmTable.TABLE_PSALMS} (${PwsPsalmTable.COLUMN_ID}));"

  private const val TABLE_DROP_SCRIPT = "drop table if exists $TABLE_PSALMPSALMREFERENCES"

  fun createTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_CREATE_SCRIPT)
  }

  fun dropTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_DROP_SCRIPT)
  }
}