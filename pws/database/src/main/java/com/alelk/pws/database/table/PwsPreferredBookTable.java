package com.alelk.pws.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by alelk on 15.03.2016.
 */
@Deprecated
public class PwsPreferredBookTable implements PwsTable {
    public static final String TABLE_PREFERRED_BOOKS = "preferredbooks";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_BOOKID = "bookid";
    public static final String COLUMN_PREFERENCE = "preference";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_PREFERRED_BOOKS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_BOOKID + " integer not null, " +
            COLUMN_PREFERENCE + " integer not null, " +
            "FOREIGN KEY (" + COLUMN_BOOKID + ") " +
            "REFERENCES " + PwsBookTable.TABLE_BOOKS + " (" +
            PwsBookTable.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_PREFERRED_BOOKS;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
