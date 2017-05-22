package com.alelk.pws.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * PWS History Table
 *
 * Created by Alex Elkin on 19.02.2016.
 */
public class PwsHistoryTable {
    public static final String TABLE_HISTORY = "history";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PSALMNUMBERID = "psalmnumberid";
    public static final String COLUMN_ACCESSTIMESTAMP = "accesstimestamp";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_HISTORY +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_PSALMNUMBERID + " integer not null, " +
            COLUMN_ACCESSTIMESTAMP + " string not null, " +
            "FOREIGN KEY (" + COLUMN_PSALMNUMBERID + ") " +
            "REFERENCES " + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " (" +
            PwsPsalmNumbersTable.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_HISTORY;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
