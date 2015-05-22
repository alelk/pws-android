package com.alelk.pws.database.provider;

import static com.alelk.pws.database.table.PwsPsalmTable.TABLE_PSALMS;
import static com.alelk.pws.database.table.PwsPsalmNumbersTable.TABLE_PSALMNUMBERS;
import static com.alelk.pws.database.table.PwsBookTable.TABLE_BOOKS;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.alelk.pws.database.helper.PwsDatabaseHelper;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;
import com.alelk.pws.database.table.PwsPsalmTable;

/**
 * Created by Alex Elkin on 21.05.2015.
 */
public class PwsDataProvider extends ContentProvider {

    // todo: move this constants
    public static final String DATABASE_NAME = "pws.db";
    public static final int DATABASE_VERSION = 5;

    public static final String AUTHORITY = "com.alelk.pws.database.provider";

    private static final int PATH_PSALMS = 1;
    private static final int PATH_PSALMS_ID = 2;
    private static final int PATH_PSALM_NUMBERS = 3;
    private static final int PATH_PSALM_NUMBERS_ID = 4;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS, PATH_PSALMS);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS + "/#", PATH_PSALMS_ID);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS + "/#/" + TABLE_PSALMNUMBERS, PATH_PSALM_NUMBERS);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS + "/#/" + TABLE_PSALMNUMBERS + "/#", PATH_PSALM_NUMBERS_ID);
    }

    private static final String TABLE_PSALMNUMBERS_JOIN_BOOKS = TABLE_PSALMNUMBERS + " AS pn " +
            "INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    private SQLiteDatabase mDatabase;
    private PwsDatabaseHelper mDatabaseHelper;

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new PwsDatabaseHelper(getContext(), DATABASE_NAME, DATABASE_VERSION);
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            @Nullable String selection,
            String[] selectionArgs,
            String sortOrder) {
        mDatabase = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = null;
        switch (sUriMatcher.match(uri)) {
            case PATH_PSALMS:
                cursor = mDatabase.query(TABLE_PSALMS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PATH_PSALM_NUMBERS:
                selection = "psalmid=?";
                selectionArgs = new String[1];
                selectionArgs[0] = uri.getPathSegments().get(1);
                cursor = mDatabase.query(TABLE_PSALMNUMBERS_JOIN_BOOKS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                // todo: throw exception - incorrect uri
        }
        if (cursor == null) {
            // todo: throw exception
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case PATH_PSALMS:
                return "vnd.android.cursor.dir/" + PwsPsalmTable.class.getName();
            case PATH_PSALMS_ID:
                return "vnd.android.cursor.item/" + PwsPsalmTable.class.getName();
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        mDatabase = mDatabaseHelper.getWritableDatabase();
        Uri itemUri = null;
        switch (sUriMatcher.match(uri)) {
            case PATH_PSALMS:
                final long id = mDatabase.insert(TABLE_PSALMS, null, values);
                itemUri = ContentUris.withAppendedId(uri, id);
                break;
            case PATH_PSALMS_ID:
                // todo throw exception: cannot insert to row
                break;

        }
        return itemUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
