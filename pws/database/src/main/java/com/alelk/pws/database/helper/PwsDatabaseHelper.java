package com.alelk.pws.database.helper;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.alelk.pws.database.table.PwsPsalmFtsTable;

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
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "pws.db";

    private static final String LOG_TAG = PwsDatabaseHelper.class.getSimpleName();
    private static final String DB_FOLDER = "/data/data/com.alelk.pws.pwapp/databases/";
    private static final String DB_PATH = DB_FOLDER + DATABASE_NAME;
    private static final String ASSETS_DB_FOLDER = "db/";

    private Context mContext;

    public PwsDatabaseHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context.getApplicationContext();
        try {
            if (!isDatabaseExists()) {
                copyDatabase();
                setUpPsalmFts();
            }
        } catch (IOException ex) {
            Log.e(LOG_TAG, "" + ": Error copying asset database files: " + ex.getLocalizedMessage());
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        final String METHOD_NAME = "onOpen";
        Log.v(LOG_TAG, METHOD_NAME + ": PWS database opened '" + DATABASE_NAME + "' version " + DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String METHOD_NAME = "onCreate";
        Log.e(LOG_TAG, METHOD_NAME + ": This method should be never called.");


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String METHOD_NAME = "onUpgrade";
        Log.e(LOG_TAG, METHOD_NAME + ": This method should be never called.");
    }

    private void copyDatabase() throws IOException {
        final String METHOD_NAME = "copyDatabase";
        AssetManager am = mContext.getAssets();
        OutputStream outputStream = null;
        InputStream inputStream = null;
        byte[] buffer = new byte[1024];
        try {
            File dbFolder = new File(DB_FOLDER);
            if (!dbFolder.exists() || !dbFolder.isDirectory()) {
                if (!dbFolder.mkdir()) {
                    Log.e(LOG_TAG, METHOD_NAME + ": Could not create directory: " + DB_FOLDER);
                }
            }
            outputStream = new FileOutputStream(DB_PATH);
            for (int i = 1; i < 20; i++) {
                try {
                    inputStream = am.open(ASSETS_DB_FOLDER + DATABASE_NAME + "." + i);
                    int count;
                    while ((count = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, count);
                    }
                    Log.i(LOG_TAG, METHOD_NAME + ": Copying success: File " + ASSETS_DB_FOLDER + DATABASE_NAME + "." + i);
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
            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }

        }
    }

    private boolean isDatabaseExists() {
        final String METHOD_NAME = "isDatabaseExists";
        File file = new File(DB_PATH);
        SQLiteDatabase database = null;
        int version = 0;
        try {
            database = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY);
            version = database.getVersion();
        } catch (SQLiteException ex) {
            Log.i(LOG_TAG, METHOD_NAME + ": Looks like database does not exists. It is needed to create. Message: " + ex.getLocalizedMessage());
        } finally {
            if (database != null) database.close();
        }
        return database != null && version == DATABASE_VERSION;
    }

    private void setUpPsalmFts() {
        final String METHOD_NAME = "setUpPsalmFts";
        SQLiteDatabase db = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READWRITE);
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
        db.close();
    }
}
