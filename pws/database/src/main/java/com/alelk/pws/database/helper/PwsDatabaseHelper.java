package com.alelk.pws.database.helper;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.alelk.pws.database.R;
import com.alelk.pws.database.table.PwsPsalmFtsTable;

import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Pws Database Helper
 *
 * Created by Alex Elkin on 29.04.2015.
 */
public class PwsDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "pws.0.9.1.db";
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

    private boolean isDatabaseExists() {
        final String METHOD_NAME = "isDatabaseExists";
        SQLiteDatabase database = null;
        int version = 0;
        try {
            database = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
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
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
        if (PwsPsalmFtsTable.isTableExists(db) && PwsPsalmFtsTable.isAllTriggersExists(db)) {
            Log.d(LOG_TAG, METHOD_NAME + ": The PWS Psalm FTS table and it's triggers are exist. No need to recreate.");
            return;
        }
        PwsPsalmFtsTable.dropAllTriggers(db);
        PwsPsalmFtsTable.dropTable(db);
        PwsPsalmFtsTable.createTable(db);
        PwsPsalmFtsTable.populateTable(db, new PwsPsalmFtsTable.UpdateProgressListener() {
            @Override
            public void onUpdateProgress(int max, int current) {
                publishProgress(R.string.txt_fts_setup, max, current);
            }
        });
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
