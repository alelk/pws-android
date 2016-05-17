package com.alelk.pws.database.helper;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.alelk.pws.database.BuildConfig;
import com.alelk.pws.database.table.PwsBookStatisticTable;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsChapterPsalmsTable;
import com.alelk.pws.database.table.PwsChapterTable;
import com.alelk.pws.database.table.PwsChorusTable;
import com.alelk.pws.database.table.PwsPsalmFtsTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;
import com.alelk.pws.database.table.PwsPsalmTable;
import com.alelk.pws.database.table.PwsVerseTable;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Alex Elkin on 29.04.2015.
 */
public class PwsDatabaseHelper extends SQLiteOpenHelper {
    private static final String LOG_TAG = PwsDatabaseHelper.class.getSimpleName();
    private static final String DB_PATH = "/data/data/com.alelk.pws.pwapp/databases/";
    private static final String ASSETS_DB_PATH = "db/";

    private String databaseName;
    private int version;
    private Context mContext;

    public PwsDatabaseHelper(@NonNull Context context, @NonNull String databaseName, int version) {
        super(context, databaseName, null, version);
        this.databaseName = databaseName;
        this.version = version;
        mContext = context.getApplicationContext();
        File file = new File(DB_PATH + databaseName);
        file.delete();
        try {
            copyDatabase();
            file = new File(DB_PATH + databaseName);
            file.setWritable(true);
        } catch (IOException ex) {
            Log.e(LOG_TAG, "" + ": Error copying asset database files: " + ex.getLocalizedMessage());
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        final String METHOD_NAME = "onOpen";
        Log.v(LOG_TAG, "PWS database opened '" + databaseName + "' version " + version);
        setUpPsalmFts(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String METHOD_NAME = "onCreate";
        Log.i(LOG_TAG, METHOD_NAME + ": Create PWS database '" + databaseName + "' version " + version);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String METHOD_NAME = "onUpgrade";
        Log.i(LOG_TAG, METHOD_NAME + ": Upgrade PWS database '" + databaseName + "' from version " + oldVersion
                + " to version " + newVersion);
        //onCreate(db);
    }

    private void copyDatabase() throws IOException {
        final String METHOD_NAME = "copyDatabase";
        AssetManager am = mContext.getAssets();
        OutputStream outputStream = null;
        InputStream inputStream = null;
        byte[] buffer = new byte[1024];
        try {
            outputStream = new FileOutputStream(DB_PATH + databaseName);
            for (int i = 1; i < 20; i++) {
                try {
                    inputStream = am.open(ASSETS_DB_PATH + databaseName + "." + i);
                    int count;
                    while ((count = inputStream.read(buffer)) != -1){
                        outputStream.write(buffer, 0, count);
                    }
                    Log.d(LOG_TAG, METHOD_NAME + ": Copying success: File " + ASSETS_DB_PATH + databaseName + "." + i);
                } catch (FileNotFoundException ex) {
                    if (i == 1) {
                        Log.w(LOG_TAG, METHOD_NAME + ": Could not open asset database file: " + ex.getLocalizedMessage());
                    }
                    return;
                } finally {
                    if (inputStream != null) inputStream.close();
                }
            }
        } catch (FileNotFoundException ex) {
            Log.w(LOG_TAG, METHOD_NAME + ": Error copying database file: " + ex.getLocalizedMessage());
        } finally {
            if (outputStream != null) outputStream.close();
        }
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
