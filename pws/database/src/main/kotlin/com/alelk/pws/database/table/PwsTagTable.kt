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
 * Created by Mykhailo Dmytriakha on 20.03.2024.
 */
object PwsTagTable {
  const val TABLE_TAG = "tags"
  const val COLUMN_ID = "_id"
  const val COLUMN_NAME = "name"
  const val COLUMN_COLOR = "color"
  const val COLUMN_PREDEFINED = "predefined"

  private const val TABLE_CREATE_SCRIPT =
    """
        CREATE TABLE $TABLE_TAG (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_NAME TEXT NOT NULL,
            $COLUMN_COLOR TEXT NOT NULL,
            $COLUMN_PREDEFINED INTEGER NOT NULL DEFAULT 0
        );
        """

  private const val TABLE_DROP_SCRIPT = "DROP TABLE IF EXISTS $TABLE_TAG"

  fun createTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_CREATE_SCRIPT)
  }

  fun dropTable(db: SQLiteDatabase) {
    db.execSQL(TABLE_DROP_SCRIPT)
  }
}
