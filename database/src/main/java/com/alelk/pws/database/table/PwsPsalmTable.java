package com.alelk.pws.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by alelkin on 21.04.2015.
 */
public class PwsPsalmTable implements PwsTable {

    public static final String TABLE_PSALMS = "psalms";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_TRANSLATOR = "translator";
    public static final String COLUMN_COMPOSER = "composer";
    public static final String COLUMN_TONALITIES = "tonalities";
    public static final String COLUMN_YEAR = "year";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_PSALMS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_VERSION + " text not null, " +
            COLUMN_NAME + " text, " +
            COLUMN_AUTHOR + " text, " +
            COLUMN_TRANSLATOR + " text, " +
            COLUMN_COMPOSER + " text, " +
            COLUMN_TONALITIES + " text, " +
            COLUMN_YEAR + " text);";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_PSALMS;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}