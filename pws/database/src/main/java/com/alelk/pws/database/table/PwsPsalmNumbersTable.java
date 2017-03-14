package com.alelk.pws.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Pws Psalm Number Table
 *
 * Created by Alex Elkin on 22.04.2015.
 */
public class PwsPsalmNumbersTable implements PwsTable {

    public static final String TABLE_PSALMNUMBERS = "psalmnumbers";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PSALMID = "psalmid";
    public static final String COLUMN_BOOKID = "bookid";
    public static final String COLUMN_NUMBER = "number";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_PSALMNUMBERS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_NUMBER + " integer not null, " +
            COLUMN_PSALMID + " integer not null, " +
            COLUMN_BOOKID + " integer not null, " +
            "FOREIGN KEY (" + COLUMN_PSALMID + ") " +
            "REFERENCES " + PwsPsalmTable.TABLE_PSALMS + " (" +
            PwsPsalmTable.COLUMN_ID + "), " +
            "FOREIGN KEY (" + COLUMN_BOOKID + ") " +
            "REFERENCES " + PwsBookTable.TABLE_BOOKS + " (" +
            PwsBookTable.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_PSALMNUMBERS;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
