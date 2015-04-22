package com.alelk.pws.database.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by alelkin on 22.04.2015.
 */
public class PwsDatabaseBookHelper extends SQLiteOpenHelper implements PwsDatabaseHelper {
    private static final String LOG_TAG = PwsDatabaseBookHelper.class.getName();

    public static final String TABLE_BOOKS = "books";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_SHORTNAME = "shortname";
    public static final String COLUMN_DISPLAYNAME = "displayname";
    public static final String COLUMN_EDITION = "edition";
    public static final String COLUMN_RELEASEDATE = "releasedate";
    public static final String COLUMN_AUTHORS = "authors";
    public static final String COLUMN_CREATORS = "creators";
    public static final String COLUMN_REVIEWERS = "reviewers";
    public static final String COLUMN_EDITORS = "editors";
    public static final String COLUMN_DESCRIPTION = "description";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_BOOKS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_VERSION + " text not null, " +
            COLUMN_NAME + " text, " +
            COLUMN_SHORTNAME + " text, " +
            COLUMN_DISPLAYNAME + " text, " +
            COLUMN_EDITION + " text not null, " +
            COLUMN_RELEASEDATE + " text, " +
            COLUMN_AUTHORS + " text, " +
            COLUMN_CREATORS + " text, " +
            COLUMN_REVIEWERS + " text, " +
            COLUMN_EDITORS + " text, " +
            COLUMN_DESCRIPTION + " text);";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_BOOKS;

    public PwsDatabaseBookHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(LOG_TAG, "Upgrade PWS database table '" + TABLE_BOOKS +
                "' from version " + oldVersion + " to version " + newVersion);
        db.execSQL(TABLE_DROP_SCRIPT);
        onCreate(db);
    }
}
