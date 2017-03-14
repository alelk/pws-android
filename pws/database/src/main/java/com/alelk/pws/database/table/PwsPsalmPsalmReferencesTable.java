package com.alelk.pws.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by alelk on 04.01.2017.
 */

public class PwsPsalmPsalmReferencesTable implements PwsTable {

    public static final String TABLE_PSALMPSALMREFERENCES = "psalmpsalmreferences";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PSALMID = "psalmid";
    public static final String COLUMN_REFPSALMID = "refpsalmid";
    public static final String COLUMN_REASON = "reason";
    public static final String COLUMN_VOLUME = "volume";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_PSALMPSALMREFERENCES +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_PSALMID + " integer not null, " +
            COLUMN_REFPSALMID + " integer not null, " +
            COLUMN_REASON + " text not null, " +
            COLUMN_VOLUME + " integer not null, " +
            "FOREIGN KEY (" + COLUMN_PSALMID + ") " +
            "REFERENCES " + PwsPsalmTable.TABLE_PSALMS + " (" +
            PwsPsalmTable.COLUMN_ID + "), " +
            "FOREIGN KEY (" + COLUMN_REFPSALMID + ") " +
            "REFERENCES " + PwsPsalmTable.TABLE_PSALMS + " (" +
            PwsPsalmTable.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_PSALMPSALMREFERENCES;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
