package com.alelk.pws.pwsdb.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.alelk.pws.pwsdb.table.PwsBookStatisticTable;
import com.alelk.pws.pwsdb.table.PwsBookTable;
import com.alelk.pws.pwsdb.table.PwsChapterPsalmsTable;
import com.alelk.pws.pwsdb.table.PwsChapterTable;
import com.alelk.pws.pwsdb.table.PwsFavoritesTable;
import com.alelk.pws.pwsdb.table.PwsHistoryTable;
import com.alelk.pws.pwsdb.table.PwsPsalmFtsTable;
import com.alelk.pws.pwsdb.table.PwsPsalmNumbersTable;
import com.alelk.pws.pwsdb.table.PwsPsalmTable;

/**
 * Created by Alex Elkin on 29.04.2015.
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
    public void onOpen(SQLiteDatabase db) {
        Log.v(LOG_TAG, "PWS database opened '" + databaseName + "' version " + version);
        //setUpPsalmFts(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String METHOD_NAME = "onCreate";
        Log.i(LOG_TAG, METHOD_NAME + ": Create PWS database '" + databaseName + "' version " + version);
        PwsBookTable.createTable(db);
        PwsPsalmTable.createTable(db);
        PwsPsalmNumbersTable.createTable(db);
        PwsChapterTable.createTable(db);
        PwsChapterPsalmsTable.createTable(db);
        PwsFavoritesTable.createTable(db);
        PwsHistoryTable.createTable(db);
        PwsBookStatisticTable.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String METHOD_NAME = "onUpgrade";
        Log.i(LOG_TAG, METHOD_NAME + ": Upgrade PWS database '" + databaseName + "' from version " + oldVersion
                + " to version " + newVersion);
        PwsBookTable.dropTable(db);
        PwsPsalmTable.dropTable(db);
        PwsPsalmNumbersTable.dropTable(db);
        PwsChapterTable.dropTable(db);
        PwsChapterPsalmsTable.dropTable(db);
        PwsFavoritesTable.dropTable(db);
        PwsHistoryTable.dropTable(db);
        PwsBookStatisticTable.dropTable(db);
        onCreate(db);
    }

    private void setUpPsalmFts(@NonNull SQLiteDatabase db) {
        final String METHOD_NAME = "setUpPsalmFts";
        if (PwsPsalmFtsTable.isTableExists(db) && PwsPsalmFtsTable.isAllTriggersExists(db)) {
            Log.d(LOG_TAG, METHOD_NAME + ": The PWS Psalm FTS table and it's triggers are exist. No need to recreate.");
            return;
        }
        PwsPsalmFtsTable.dropAllTriggers(db);
        PwsPsalmFtsTable.dropTable(db);
        PwsPsalmFtsTable.createTable(db);
        PwsPsalmFtsTable.populateTable(db);
        PwsPsalmFtsTable.setUpAllTriggers(db);
        Log.i(LOG_TAG, METHOD_NAME + ": The PWS Psalm FTS table has been created and populated. All needed triggers are setting up.");
    }
}
