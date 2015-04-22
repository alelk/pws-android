package com.alelk.pws.database.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by alelkin on 21.04.2015.
 */
public class PwsDatabasePsalmHelper extends SQLiteOpenHelper implements PwsDatabaseHelper{
    private static final String LOG_TAG = PwsDatabasePsalmHelper.class.getName();

    public static final String TABLE_PSALMS = "psalms";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_TRANSLATOR = "translator";
    public static final String COLUMN_COMPOSER = "composer";
    public static final String COLUMN_TONALITIES = "tonalities";
    public static final String COLUMN_YEAR = "year";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_PSALMS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_VERSION + " text not null, " +
            COLUMN_NAME + " text, " +
            COLUMN_AUTHOR + " text, " +
            COLUMN_TRANSLATOR + " text, " +
            COLUMN_COMPOSER + " text, " +
            COLUMN_TONALITIES + " text, " +
            COLUMN_YEAR + " text);";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_PSALMS;

    public PwsDatabasePsalmHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(LOG_TAG, "Upgrade PWS database table '" + TABLE_PSALMS +
                "' from version " + oldVersion + " to version " + newVersion);
        db.execSQL(TABLE_DROP_SCRIPT);
        onCreate(db);
    }
}
