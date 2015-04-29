package com.alelk.pws.database.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsChapterPsalmsTable;
import com.alelk.pws.database.table.PwsChapterTable;
import com.alelk.pws.database.table.PwsChorusTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;
import com.alelk.pws.database.table.PwsPsalmTable;
import com.alelk.pws.database.table.PwsVerseTable;

/**
 * Created by alelkin on 29.04.2015.
 */
public class PwsDatabaseHelper extends SQLiteOpenHelper {
    private static final String LOG_TAG = PwsDatabaseHelper.class.getSimpleName();

    private String databaseName;
    private int version;

    public PwsDatabaseHelper(Context context, String databaseName, int version) {
        super(context, databaseName, null, version);
        this.databaseName = databaseName;
        this.version = version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(LOG_TAG, "Create PWS database '" + databaseName + "' version " + version);
        PwsBookTable.createTable(db);
        PwsPsalmTable.createTable(db);
        PwsPsalmNumbersTable.createTable(db);
        PwsVerseTable.createTable(db);
        PwsChorusTable.createTable(db);
        PwsChapterTable.createTable(db);
        PwsChapterPsalmsTable.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(LOG_TAG, "Upgrade PWS database '" + databaseName + "' from version " + oldVersion
                + " to version " + newVersion);
        PwsBookTable.dropTable(db);
        PwsPsalmTable.dropTable(db);
        PwsPsalmNumbersTable.dropTable(db);
        PwsVerseTable.dropTable(db);
        PwsChorusTable.dropTable(db);
        PwsChapterTable.dropTable(db);
        PwsChapterPsalmsTable.dropTable(db);
        onCreate(db);
    }
}
