package com.alelk.pws.database.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by alelkin on 21.04.2015.
 */
public class PwsDatabaseChapterHelper extends SQLiteOpenHelper implements PwsDatabaseHelper {
    private static final String LOG_TAG = PwsDatabasePsalmHelper.class.getName();

    public static final String TABLE_CHAPTERS = "chapters";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_BOOKID = "bookid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_SHORTNAME = "shortname";
    public static final String COLUMN_DISPLAYNAME = "displayname";
    public static final String COLUMN_RELEASEDATE = "releasedate";
    public static final String COLUMN_DESCRIPTION = "releasedate";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_CHAPTERS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_VERSION + " text not null, " +
            COLUMN_BOOKID + " integer not null " +
            COLUMN_NAME + " text, " +
            COLUMN_SHORTNAME + " text, " +
            COLUMN_DISPLAYNAME + " text, " +
            COLUMN_RELEASEDATE + " text, " +
            COLUMN_DESCRIPTION + " text);";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_CHAPTERS;

    public PwsDatabaseChapterHelper(Context context, SQLiteDatabase.CursorFactory factory) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(LOG_TAG, "Upgrade PWS database table '" + TABLE_CHAPTERS +
                "' from version " + oldVersion + " to version " + newVersion);
        db.execSQL(TABLE_DROP_SCRIPT);
        onCreate(db);
    }
}