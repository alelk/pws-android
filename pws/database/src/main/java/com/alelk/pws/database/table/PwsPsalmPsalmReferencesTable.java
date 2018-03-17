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

import android.database.sqlite.SQLiteDatabase;

/**
 * PWS Psalm Psalm Reference Table
 *
 * Created by alelk on 04.01.2017.
 */

public class PwsPsalmPsalmReferencesTable implements PwsTable {

    public static final String TABLE_PSALMPSALMREFERENCES = "psalmpsalmreferences";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PSALMID = "psalmid";
    public static final String COLUMN_REFPSALMID = "refpsalmid";
    public static final String COLUMN_REASON = "reason";
    public static final String COLUMN_VOLUME = "volume";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_PSALMPSALMREFERENCES +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_PSALMID + " integer not null, " +
            COLUMN_REFPSALMID + " integer not null, " +
            COLUMN_REASON + " text not null, " +
            COLUMN_VOLUME + " integer not null, " +
            "FOREIGN KEY (" + COLUMN_PSALMID + ") " +
            "REFERENCES " + PwsPsalmTable.TABLE_PSALMS + " (" +
            PwsPsalmTable.COLUMN_ID + "), " +
            "FOREIGN KEY (" + COLUMN_REFPSALMID + ") " +
            "REFERENCES " + PwsPsalmTable.TABLE_PSALMS + " (" +
            PwsPsalmTable.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_PSALMPSALMREFERENCES;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
