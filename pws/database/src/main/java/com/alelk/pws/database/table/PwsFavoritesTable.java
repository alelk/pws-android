package com.alelk.pws.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Alex Elkin on 19.02.2016.
 */
public class PwsFavoritesTable implements PwsTable {
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_POSITION = "position";
    public static final String COLUMN_PSALMNUMBERID = "psalmnumberid";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_FAVORITES +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_POSITION + " integer unique not null, " +
            COLUMN_PSALMNUMBERID + " integer not null, " +
            "FOREIGN KEY (" + COLUMN_PSALMNUMBERID + ") " +
            "REFERENCES " + PwsPsalmNumbersTable.TABLE_PSALMNUMBERS + " (" +
            PwsPsalmNumbersTable.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_FAVORITES;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
