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
 * Pws Book Table
 *
 * Created by Alex Elkin on 22.04.2015.
 */
public class PwsBookTable implements PwsTable {

    public static final String TABLE_BOOKS = "books";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DISPLAYSHORTNAME = "displayshortname";
    public static final String COLUMN_DISPLAYNAME = "displayname";
    public static final String COLUMN_EDITION = "edition";
    public static final String COLUMN_RELEASEDATE = "releasedate";
    public static final String COLUMN_AUTHORS = "authors";
    public static final String COLUMN_CREATORS = "creators";
    public static final String COLUMN_REVIEWERS = "reviewers";
    public static final String COLUMN_EDITORS = "editors";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_LOCALE = "locale";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_BOOKS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_VERSION + " text not null, " +
            COLUMN_LOCALE + " text, " +
            COLUMN_NAME + " text, " +
            COLUMN_DISPLAYSHORTNAME + " text, " +
            COLUMN_DISPLAYNAME + " text, " +
            COLUMN_EDITION + " text not null, " +
            COLUMN_RELEASEDATE + " text, " +
            COLUMN_AUTHORS + " text, " +
            COLUMN_CREATORS + " text, " +
            COLUMN_REVIEWERS + " text, " +
            COLUMN_EDITORS + " text, " +
            COLUMN_DESCRIPTION + " text);";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_BOOKS;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
