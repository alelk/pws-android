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
 * Pws Chapter Table
 *
 * Created by alelkin on 21.04.2015.
 */
public class PwsChapterTable implements PwsTable {

    public static final String TABLE_CHAPTERS = "chapters";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_BOOKID = "bookid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_SHORTNAME = "shortname";
    public static final String COLUMN_DISPLAYNAME = "displayname";
    public static final String COLUMN_RELEASEDATE = "releasedate";
    public static final String COLUMN_DESCRIPTION = "description";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_CHAPTERS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_VERSION + " text not null, " +
            COLUMN_BOOKID + " integer not null, " +
            COLUMN_NAME + " text, " +
            COLUMN_SHORTNAME + " text, " +
            COLUMN_DISPLAYNAME + " text, " +
            COLUMN_RELEASEDATE + " text, " +
            COLUMN_DESCRIPTION + " text);";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_CHAPTERS;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
