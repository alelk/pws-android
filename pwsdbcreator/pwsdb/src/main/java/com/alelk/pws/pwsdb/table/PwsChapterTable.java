package com.alelk.pws.pwsdb.table;

import android.database.sqlite.SQLiteDatabase;

/**
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
