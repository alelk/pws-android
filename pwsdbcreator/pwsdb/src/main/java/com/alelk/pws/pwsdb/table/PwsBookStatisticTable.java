package com.alelk.pws.pwsdb.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Alex Elkin on 17.03.2016.
 */
public class PwsBookStatisticTable implements PwsTable {
    public static final String TABLE_BOOKSTATISTIC = "bookstatistic";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_BOOKID = "bookid";
    public static final String COLUMN_USERPREFERENCE = "userpref";
    public static final String COLUMN_READINGS = "readings";
    public static final String COLUMN_RATING = "rating";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_BOOKSTATISTIC +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_BOOKID + " integer not null, " +
            COLUMN_USERPREFERENCE + " integer, " +
            COLUMN_READINGS + " integer, " +
            COLUMN_RATING + " integer, " +
            "FOREIGN KEY (" + COLUMN_BOOKID + ") " +
            "REFERENCES " + PwsBookTable.TABLE_BOOKS + " (" +
            PwsBookTable.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_BOOKSTATISTIC;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
