package com.alelk.pws.database.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by alelkin on 22.04.2015.
 */
public class PwsDatabaseChapterPsalmsHelper extends SQLiteOpenHelper implements PwsDatabaseHelper {
    private static final String LOG_TAG = PwsDatabaseChapterPsalmsHelper.class.getName();

    public static final String TABLE_CHAPTERPSALMS = "chapterpsalms";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PSALMID = "psalmid";
    public static final String COLUMN_CHAPTERID = "chapterid";

    private static final String TABLE_CREATE_SCRIPT = "create table " + TABLE_CHAPTERPSALMS +
            "(" + COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_PSALMID + " integer not null, " +
            COLUMN_CHAPTERID + " integer not null, " +
            "FOREIGN KEY (" + COLUMN_PSALMID + ") " +
            "REFERENCES " + PwsDatabasePsalmHelper.TABLE_PSALMS + "(" +
            PwsDatabasePsalmHelper.COLUMN_ID + ")" +
            "FOREIGN KEY (" + COLUMN_CHAPTERID + ") " +
            "REFERENCES " + PwsDatabaseChapterHelper.TABLE_CHAPTERS + "(" +
            PwsDatabaseChapterHelper.COLUMN_ID + "));";

    private static final String TABLE_DROP_SCRIPT = "drop table if exists " + TABLE_CHAPTERPSALMS;

    public PwsDatabaseChapterPsalmsHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_SCRIPT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(LOG_TAG, "Upgrade PWS database table '" + TABLE_CHAPTERPSALMS +
                "' from version " + oldVersion + " to version " + newVersion);
        db.execSQL(TABLE_DROP_SCRIPT);
        onCreate(db);
    }
}
