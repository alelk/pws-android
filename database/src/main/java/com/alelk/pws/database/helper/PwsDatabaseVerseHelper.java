package com.alelk.pws.database.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by alelkin on 22.04.2015.
 */
public class PwsDatabaseVerseHelper extends SQLiteOpenHelper implements PwsDatabaseHelper {
    private static final String LOG_TAG = PwsDatabaseVerseHelper.class.getName();

    public static final String TABLE_VERSES = "verses";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NUMBER = "number";
    public static final String COLUMN_PSALMID = "psalmid";
    public static final String COLUMN_TEXT = "text";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_VERSES +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_NUMBER + " text not null, " +
            COLUMN_TEXT + " text, " +
            COLUMN_PSALMID + " integer not null, " +
            "FOREIGN KEY (" + COLUMN_PSALMID + ") " +
            "REFERENCES " + PwsDatabasePsalmHelper.TABLE_PSALMS + " (" +
            PwsDatabasePsalmHelper.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_VERSES;

    public PwsDatabaseVerseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(LOG_TAG, "Upgrade PWS database table '" + TABLE_VERSES +
                "' from version " + oldVersion + " to version " + newVersion);
        db.execSQL(TABLE_DROP_SCRIPT);
        onCreate(db);
    }
}
