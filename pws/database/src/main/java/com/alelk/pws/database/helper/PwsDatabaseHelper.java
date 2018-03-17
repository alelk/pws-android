/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alelk.pws.database.helper;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.alelk.pws.database.R;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.table.PwsPsalmFtsTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.alelk.pws.database.table.PwsFavoritesTable.TABLE_FAVORITES;
import static com.alelk.pws.database.table.PwsHistoryTable.TABLE_HISTORY;

/**
 * Pws Database Helper
 *
 * Created by Alex Elkin on 29.04.2015.
 */
public class PwsDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "pws.1.2.0.db";
    private static final String[] DATABASE_PREVIOUS_NAMES = {"pws.1.1.0.db", "pws.0.9.1.db"};
    private static final int DATABASE_VERSION_091 = 1;
    private static final int DATABASE_VERSION_110 = 2;
    private static final int DB_INIT_NOTIFICATION_ID = 1331;

    private static final String LOG_TAG = PwsDatabaseHelper.class.getSimpleName();
    private final String dbFolder;
    private final String dbPath;
    private static final String ASSETS_DB_FOLDER = "db";

    private final Context mContext;
    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;

    public PwsDatabaseHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
        dbPath = context.getDatabasePath(DATABASE_NAME).getPath();
        dbFolder = context.getDatabasePath(DATABASE_NAME).getParent() + "/";
        if (!isDatabaseExists()) {
            Log.i(LOG_TAG, "PwsDatabaseHelper: The current version of database does not exists. Looks like it is " +
                    "first app starting. Trying to create database..");
            try {
                mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationBuilder = new NotificationCompat.Builder(mContext);
                mNotificationBuilder.setContentTitle(mContext.getString(R.string.txt_title_database_init))
                        .setSmallIcon(R.drawable.ic_data_usage_black)
                        .setTicker(mContext.getString(R.string.txt_title_database_init));
                copyDatabase();
                if (!isDatabaseExists()) {
                    Log.e(LOG_TAG, "PwsDatabaseHelper: Database was not be copied from asset folder: Database does not exists: " + dbPath);
                    return;
                }
                setUpPsalmFts();
                mergePreviousDatabase();
                removePreviousDatabaseIfExists();
            } catch (IOException e) {
                Log.e(LOG_TAG, "PwsDatabaseHelper: Error copying database file: " + e.getLocalizedMessage());
            } finally {
                if (mNotificationManager != null)
                    mNotificationManager.cancel(DB_INIT_NOTIFICATION_ID);
            }
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
            File dbFolder = new File(this.dbFolder);
            if (!dbFolder.exists() || !dbFolder.isDirectory()) {
                if (!dbFolder.mkdir()) {
                    Log.e(LOG_TAG, METHOD_NAME + ": Could not create directory: " + this.dbFolder);
                }
            }
            String[] fileList = am.list(ASSETS_DB_FOLDER);
            if (fileList == null || fileList.length == 0) {
                Log.e(LOG_TAG, METHOD_NAME + ": No database files found in asset directory " + ASSETS_DB_FOLDER);
                return;
            }
            outputStream = new FileOutputStream(dbPath);
            for (int i = 1; i <= fileList.length; i++) {
                publishProgress(R.string.txt_copy_files, fileList.length, i);
                try {
                    inputStream = am.open(ASSETS_DB_FOLDER + "/" + DATABASE_NAME + "." + i);
                    int count;
                    while ((count = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, count);
                    }
                    Log.i(LOG_TAG, METHOD_NAME + ": Copying success: File " + ASSETS_DB_FOLDER + "/" + DATABASE_NAME + "." + i);
                } catch (FileNotFoundException ex) {
                    if (i == 1) {
                        Log.w(LOG_TAG, METHOD_NAME + ": Could not open asset database file: " + ex.getLocalizedMessage());
                    }
                    return;
                } finally {
                    try {
                        if (inputStream != null) inputStream.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, METHOD_NAME + ": Error closing input stream: " + e.getLocalizedMessage());
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            Log.w(LOG_TAG, METHOD_NAME + ": Error copying database file: " + ex.getLocalizedMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, METHOD_NAME + ": Error closing output stream: " + e.getLocalizedMessage());
                }
            }
        }
    }

    private SQLiteDatabase openPreviousDatabaseIfExists() {
        for (String dbName : DATABASE_PREVIOUS_NAMES) {
            final String databasePath = mContext.getDatabasePath(dbName).getPath();
            SQLiteDatabase database = openDatabase(databasePath, SQLiteDatabase.OPEN_READONLY);
            if (database != null) return database;
        }
        return null;
    }
    private void removePreviousDatabaseIfExists() {
        for (String dbName : DATABASE_PREVIOUS_NAMES) {
            final String databasePath = mContext.getDatabasePath(dbName).getPath();
            File file = new File(databasePath);
            if (file.exists() && file.isFile()) {
                Log.i(LOG_TAG, "removePreviousDatabaseIfExists: Previous version of database will be removed: "
                + databasePath);
                file.delete();
                return;
            }
        }
    }

    private void mergePreviousDatabase() {
        final String METHOD_NAME = "mergePreviousDatabase";
        SQLiteDatabase prevDatabase = openPreviousDatabaseIfExists();
        if (prevDatabase == null) return;
        SQLiteDatabase database = openDatabase(dbPath, SQLiteDatabase.OPEN_READWRITE);
        if (database == null) {
            Log.e(LOG_TAG, METHOD_NAME + ": Could not open database + " + dbPath);
            return;
        }
        Log.i(LOG_TAG, METHOD_NAME + ": Merge user data from database " +
                prevDatabase.getPath() + " (version " + prevDatabase.getVersion() + ") to " +
                database.getPath() + " (version " + database.getVersion() + ")");
        try {
            switch (prevDatabase.getVersion()) {
                case DATABASE_VERSION_091:
                case DATABASE_VERSION_110:
                    Cursor cursor = queryFavorites091(prevDatabase);
                    Log.v(LOG_TAG, METHOD_NAME + ": Count of favorites will be merged from previous database: " + cursor.getCount());
                    insertFavorites(database, cursor);
                    cursor.close();
                    cursor = queryHistory091(prevDatabase);
                    Log.v(LOG_TAG, METHOD_NAME + ": Count of history items will be merged from previous database: " + cursor.getCount());
                    insertHistory(database, cursor);
                    cursor.close();
                    break;
                default:
                    Log.e(LOG_TAG, METHOD_NAME + ": Unexpected database version: " + prevDatabase.getVersion());
            }
        } finally {
            prevDatabase.close();
            database.close();
        }
    }

    private Cursor queryFavorites091(SQLiteDatabase db) {
        return db.query(false,
                "favorites as f inner join psalmnumbers as pn on f.psalmnumberid=pn._id" +
                " inner join books as b on pn.bookid=b._id",
                new String[]{"f.position as position", "pn.number as number", "b.edition as edition"},
                null ,null, null, null, null, null);
    }

    private Cursor queryHistory091(SQLiteDatabase db) {
        return db.query(false,
                "history as h inner join psalmnumbers as pn on h.psalmnumberid=pn._id" +
                        " inner join books as b on pn.bookid=b._id",
                new String[]{"h.accesstimestamp as accesstimestamp", "pn.number as number", "b.edition as edition"},
                null ,null, null, null, null, null);
    }

    private void insertFavorites(SQLiteDatabase db, Cursor cursor) {
        final String METHOD_NAME = "insertFavorites";
        if (!cursor.moveToFirst()) {
            Log.d(LOG_TAG, METHOD_NAME + ": unable insert favorites: no favorites found in previous db");
            return;
        }
        do {
            Cursor current = null;
            try {
                final String edition = cursor.getString(cursor.getColumnIndex("edition"));
                final int number = cursor.getInt(cursor.getColumnIndex("number"));
                current = db.query(false, PwsDataProviderContract.TABLE_PSALMNUMBERS_JOIN_BOOKS,
                        PwsDataProviderContract.PsalmNumbers.PROJECTION,
                        "b." + PwsBookTable.COLUMN_EDITION + " like '" + edition +
                                "' and pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + "=" + number,
                        null, null, null, null, null);
                if (!current.moveToFirst()) {
                    Log.w(LOG_TAG, METHOD_NAME + ": cannot find psalm with number=" + number +
                            " and edition=" + edition + " in current database.");
                    continue;
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(PwsFavoritesTable.COLUMN_POSITION, cursor.getLong(cursor.getColumnIndex("position")));
                contentValues.put(PwsFavoritesTable.COLUMN_PSALMNUMBERID, current.getLong(current.getColumnIndex(PwsDataProviderContract.PsalmNumbers.COLUMN_PSALMNUMBER_ID)));
                Log.v(LOG_TAG, METHOD_NAME + ": Inserted new item to favorites: edition=" + edition
                        + " number=" + number);
            } finally {
                if (current != null) current.close();
            }

        } while (cursor.moveToNext());
    }

    private void insertHistory(SQLiteDatabase db, Cursor cursor) {
        final String METHOD_NAME = "insertHistory";
        if (!cursor.moveToFirst()) {
            Log.d(LOG_TAG, METHOD_NAME + ": unable insert history: no history items found in previous db");
            return;
        }
        do {
            Cursor current = null;
            try {
                final String edition = cursor.getString(cursor.getColumnIndex("edition"));
                final int number = cursor.getInt(cursor.getColumnIndex("number"));
                current = db.query(false, PwsDataProviderContract.TABLE_PSALMNUMBERS_JOIN_BOOKS,
                        PwsDataProviderContract.PsalmNumbers.PROJECTION,
                        "b." + PwsBookTable.COLUMN_EDITION + " like '" + edition +
                                "' and pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + "=" + number,
                        null, null, null, null, null);
                if (!current.moveToFirst()) {
                    Log.w(LOG_TAG, METHOD_NAME + ": cannot find psalm with number=" + number +
                            " and edition=" + edition + " in current database.");
                    continue;
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(PwsHistoryTable.COLUMN_ACCESSTIMESTAMP, cursor.getString(cursor.getColumnIndex("accesstimestamp")));
                contentValues.put(PwsHistoryTable.COLUMN_PSALMNUMBERID, current.getLong(current.getColumnIndex(PwsDataProviderContract.PsalmNumbers.COLUMN_PSALMNUMBER_ID)));
                Log.v(LOG_TAG, METHOD_NAME + ": Inserted new item to history: edition=" + edition
                        + " number=" + number);
            } finally {
                if (current != null) current.close();
            }

        } while (cursor.moveToNext());
    }

    private boolean isDatabaseExists() {
        File file = new File(dbPath);
        return file.exists() && file.isFile();
    }

    private SQLiteDatabase openDatabase(String dbPath, int flags) {
        try {
            return SQLiteDatabase.openDatabase(dbPath, null, flags);
        } catch (SQLiteException ex) {
            Log.i(LOG_TAG, "openDatabase: Database " + dbPath + " does not exists. Message: " + ex.getLocalizedMessage());
        }
        return null;
    }

    private void setUpPsalmFts() {
        final String METHOD_NAME = "setUpPsalmFts";
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
        if (PwsPsalmFtsTable.isTableExists(db) && PwsPsalmFtsTable.isAllTriggersExists(db)) {
            Log.d(LOG_TAG, METHOD_NAME + ": The PWS Psalm FTS table and it's triggers are exist. No need to recreate.");
            return;
        }
        PwsPsalmFtsTable.dropAllTriggers(db);
        PwsPsalmFtsTable.dropTable(db);
        PwsPsalmFtsTable.createTable(db);
        PwsPsalmFtsTable.populateTable(db, (max, current) -> publishProgress(R.string.txt_fts_setup, max, current));
        PwsPsalmFtsTable.setUpAllTriggers(db);
        Log.i(LOG_TAG, METHOD_NAME + ": The PWS Psalm FTS table has been created and populated. All needed triggers are setting up.");
        db.close();
    }

    private void publishProgress(int resourceId, int max, int current) {
        if (mNotificationBuilder == null || mNotificationManager == null) {
            Log.w(LOG_TAG, "publishProgress: cannot show notification");
            return;
        }
        mNotificationBuilder.setContentText(String.format(mContext.getString(resourceId), current, max));
        mNotificationBuilder.setProgress(max, current, false);
        mNotificationManager.notify(DB_INIT_NOTIFICATION_ID, mNotificationBuilder.build());
    }
}
