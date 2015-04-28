package com.alelk.pws.database.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by alelkin on 22.04.2015.
 */
public class PwsDatabasePsalmNumbersHelper extends SQLiteOpenHelper implements PwsDatabaseHelper {
    private static final String LOG_TAG = PwsDatabasePsalmNumbersHelper.class.getName();

    public static final String TABLE_PSALMNUMBERS = "psalmnumbers";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PSALMID = "psalmid";
    public static final String COLUMN_BOOKID = "bookid";
    public static final String COLUMN_NUMBER = "number";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_PSALMNUMBERS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_NUMBER + " integer not null, " +
            COLUMN_PSALMID + " integer not null, " +
            COLUMN_BOOKID + " integer not null, " +
            "FOREIGN KEY (" + COLUMN_PSALMID + ") " +
            "REFERENCES " + PwsDatabasePsalmHelper.TABLE_PSALMS + " (" +
            PwsDatabasePsalmHelper.COLUMN_ID + "), " +
            "FOREIGN KEY (" + COLUMN_BOOKID + ") " +
            "REFERENCES " + PwsDatabaseBookHelper.TABLE_BOOKS + " (" +
            PwsDatabaseBookHelper.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_PSALMNUMBERS;

    public PwsDatabasePsalmNumbersHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(LOG_TAG, "Upgrade PWS database table '" + TABLE_PSALMNUMBERS +
                "' from version " + oldVersion + " to version " + newVersion);
        db.execSQL(TABLE_DROP_SCRIPT);
        onCreate(db);
    }
}
