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

package com.alelk.pws.database.table;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

/**
 * Pws Table Helper
 *
 * Created by Alex Elkin on 16.03.2016.
 */
abstract class PwsTableHelper {
    private static final String CHECK_EXISTENCE_SCRIPT = "select count (*) " +
            "from sqlite_master " +
            "where type=? and name=?";

    static boolean isTableExists(@NonNull SQLiteDatabase db, @NonNull String tableName) {
        return isTypeExists(db, "table", tableName);
    }

    static boolean isTriggerExists(@NonNull SQLiteDatabase db, @NonNull String triggerName) {
        return isTypeExists(db, "trigger", triggerName);
    }

    private static boolean isTypeExists(@NonNull SQLiteDatabase db, @NonNull String type, @NonNull String name) {
        Cursor cursor = db.rawQuery(CHECK_EXISTENCE_SCRIPT, new String[] {type, name});
        if (!cursor.moveToFirst()) return false;
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }
}
