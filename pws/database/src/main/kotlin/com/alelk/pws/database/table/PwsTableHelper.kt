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
 * Pws Table Helper
 *
 * Created by Alex Elkin on 16.03.2016.
 */
internal abstract class PwsTableHelper {
  companion object {
    private const val CHECK_EXISTENCE_SCRIPT = "select count (*) from sqlite_master where type=? and name=?"

    @JvmStatic
    fun isTableExists(db: SQLiteDatabase, tableName: String): Boolean {
      return isTypeExists(db, "table", tableName)
    }

    @JvmStatic
    fun isTriggerExists(db: SQLiteDatabase, triggerName: String): Boolean {
      return isTypeExists(db, "trigger", triggerName)
    }

    private fun isTypeExists(db: SQLiteDatabase, type: String, name: String): Boolean {
      val cursor = db.rawQuery(CHECK_EXISTENCE_SCRIPT, arrayOf(type, name))
      if (!cursor.moveToFirst()) return false
      val count = cursor.getInt(0)
      cursor.close()
      return count > 0
    }
  }
}