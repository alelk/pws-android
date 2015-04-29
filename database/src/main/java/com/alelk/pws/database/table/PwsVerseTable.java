package com.alelk.pws.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by alelkin on 22.04.2015.
 */
public class PwsVerseTable implements PwsTable {

    public static final String TABLE_VERSES = "verses";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NUMBER = "number";
    public static final String COLUMN_PSALMID = "psalmid";
    public static final String COLUMN_TEXT = "text";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_VERSES +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_NUMBER + " text not null, " +
            COLUMN_TEXT + " text, " +
            COLUMN_PSALMID + " integer not null, " +
            "FOREIGN KEY (" + COLUMN_PSALMID + ") " +
            "REFERENCES " + PwsPsalmTable.TABLE_PSALMS + " (" +
            PwsPsalmTable.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_VERSES;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
