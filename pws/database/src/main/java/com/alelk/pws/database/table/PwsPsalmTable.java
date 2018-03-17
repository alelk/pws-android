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
 * Pws Psalm Table
 *
 * Created by Alex Elkin on 21.04.2015.
 */
public class PwsPsalmTable implements PwsTable {

    public static final String TABLE_PSALMS = "psalms";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_LOCALE = "locale";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_TRANSLATOR = "translator";
    public static final String COLUMN_COMPOSER = "composer";
    public static final String COLUMN_TONALITIES = "tonalities";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_ANNOTATION = "bibleref";
    public static final String COLUMN_TEXT = "text";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_PSALMS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_VERSION + " text not null, " +
            COLUMN_LOCALE + " text, " +
            COLUMN_NAME + " text, " +
            COLUMN_AUTHOR + " text, " +
            COLUMN_TRANSLATOR + " text, " +
            COLUMN_COMPOSER + " text, " +
            COLUMN_TONALITIES + " text, " +
            COLUMN_YEAR + " text, " +
            COLUMN_ANNOTATION + " text, " +
            COLUMN_TEXT + " text not null);";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_PSALMS;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
