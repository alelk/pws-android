package com.alelk.pws.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by alelkin on 22.04.2015.
 */
public class PwsChapterPsalmsTable implements PwsTable {

    public static final String TABLE_CHAPTERPSALMS = "chapterpsalms";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PSALMID = "psalmid";
    public static final String COLUMN_CHAPTERID = "chapterid";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_CHAPTERPSALMS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_PSALMID + " integer not null, " +
            COLUMN_CHAPTERID + " integer not null, " +
            "FOREIGN KEY (" + COLUMN_PSALMID + ") " +
            "REFERENCES " + PwsPsalmTable.TABLE_PSALMS + "(" +
            PwsPsalmTable.COLUMN_ID + ")" +
            "FOREIGN KEY (" + COLUMN_CHAPTERID + ") " +
            "REFERENCES " + PwsChapterTable.TABLE_CHAPTERS + "(" +
            PwsChapterTable.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_CHAPTERPSALMS;

    public static void createTable(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL(TABLE_DROP_SCRIPT);
    }
}
