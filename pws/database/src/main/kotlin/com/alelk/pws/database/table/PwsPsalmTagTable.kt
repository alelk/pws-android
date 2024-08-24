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
 * PWS Psalm Tag Table
 *
 * Created by Mykhailo Dmytriakha on 20.03.2024.
 */
object PwsPsalmTagTable {
  const val TABLE_PSALM_TAG = "psalm_tag"
  const val COLUMN_PSALM_NUMBER_ID = "psalmnumbers_id"
  const val COLUMN_TAG_ID = "tag_id"

  private const val TABLE_CREATE_SCRIPT = """
        CREATE TABLE $TABLE_PSALM_TAG (
            $COLUMN_PSALM_NUMBER_ID INTEGER NOT NULL,
            $COLUMN_TAG_ID INTEGER NOT NULL,
            PRIMARY KEY (COLUMN_PSALM_ID, COLUMN_TAG_ID),
            FOREIGN KEY ($COLUMN_PSALM_NUMBER_ID) REFERENCES ${PwsPsalmTable.TABLE_PSALMS} (${PwsPsalmTable.COLUMN_ID}),
            FOREIGN KEY ($COLUMN_TAG_ID) REFERENCES ${PwsTagTable.TABLE_TAG} (${PwsTagTable.COLUMN_ID})
        );
    """

  private const val TABLE_DROP_SCRIPT = "DROP TABLE IF EXISTS $TABLE_PSALM_TAG"

  fun createTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_CREATE_SCRIPT)
  }

  fun dropTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_DROP_SCRIPT)
  }
}