package com.alelk.pws.database.provider;

import static com.alelk.pws.database.table.PwsPsalmTable.TABLE_PSALMS;
import static com.alelk.pws.database.table.PwsPsalmNumbersTable.TABLE_PSALMNUMBERS;
import static com.alelk.pws.database.table.PwsBookTable.TABLE_BOOKS;
import static android.app.SearchManager.*;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.alelk.pws.database.helper.PwsDatabaseHelper;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;
import com.alelk.pws.database.table.PwsPsalmTable;

/**
 * Created by Alex Elkin on 21.05.2015.
 */
public class PwsDataProvider extends ContentProvider {

    private static final String LOG_TAG = PwsDataProvider.class.getSimpleName();

    // todo: move this constants
    public static final String DATABASE_NAME = "pws.db";
    public static final int DATABASE_VERSION = 9;

    public static final String AUTHORITY = "com.alelk.pws.database.provider";

    private static final int PATH_PSALMS = 1;
    private static final int PATH_PSALMS_ID = 2;
    private static final int PATH_PSALM_NUMBERS = 3;
    private static final int PATH_PSALM_NUMBERS_ID = 4;
    private static final int PATH_PSALMS_SUGGESTIONS_NUMBER = 5;
    private static final int PATH_PSALMS_SUGGESTIONS_NAME = 6;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS, PATH_PSALMS);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS + "/#", PATH_PSALMS_ID);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS + "/#/" + TABLE_PSALMNUMBERS, PATH_PSALM_NUMBERS);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS + "/#/" + TABLE_PSALMNUMBERS + "/#", PATH_PSALM_NUMBERS_ID);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY + "/#", PATH_PSALMS_SUGGESTIONS_NUMBER);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY + "/*", PATH_PSALMS_SUGGESTIONS_NAME);
    }

    private static final String TABLE_PSALMNUMBERS_JOIN_BOOKS = TABLE_PSALMNUMBERS + " AS pn " +
            "INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    private static final String TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS = TABLE_PSALMS + " AS p " +
            "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
            "ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID +
            " INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    private static final String[] DEFAULT_PSALMNUMBERS_PROJECTION = {
            PwsPsalmNumbersTable.COLUMN_PSALMID,
            PwsPsalmNumbersTable.COLUMN_NUMBER,
            PwsPsalmNumbersTable.COLUMN_BOOKID,
            PwsBookTable.COLUMN_EDITION,
            PwsBookTable.COLUMN_DISPLAYNAME,
            PwsBookTable.COLUMN_NAME
    };

    private static final String[] SUGGESTIONS_PSALMS_PROJECTION = {
            PwsPsalmTable.COLUMN_ID,
            PwsPsalmTable.COLUMN_NAME + " AS " + SUGGEST_COLUMN_TEXT_1,
            PwsPsalmTable.COLUMN_ID + " AS " + SUGGEST_COLUMN_INTENT_DATA_ID
    };

    private static final String[] SUGGESTIONS_PSALM_NUMBERS_PROJECTION = {
            "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS _id ",
            "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + PwsPsalmNumbersTable.COLUMN_NUMBER,
            "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + SUGGEST_COLUMN_TEXT_2,
            "p." + PwsPsalmTable.COLUMN_NAME + " AS " + SUGGEST_COLUMN_TEXT_1,
            "p." + PwsPsalmTable.COLUMN_ID + " AS " + SUGGEST_COLUMN_INTENT_DATA_ID,
            "b." + PwsBookTable.COLUMN_EDITION + " AS " + SUGGEST_COLUMN_INTENT_EXTRA_DATA
    };

    private SQLiteDatabase mDatabase;
    private PwsDatabaseHelper mDatabaseHelper;
    private String mSelection;
    private String mLimit;
    private Cursor mCursor;
    private String[] mSelectionArgs;

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new PwsDatabaseHelper(getContext(), DATABASE_NAME, DATABASE_VERSION);
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        final String METHOD_NAME = "query";
        Log.v(LOG_TAG, METHOD_NAME + ": uri='" + uri.toString() + "'");
        mDatabase = mDatabaseHelper.getReadableDatabase();
        mCursor = null;
        switch (sUriMatcher.match(uri)) {
            case PATH_PSALMS:
                mCursor = mDatabase.query(TABLE_PSALMS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PATH_PSALMS_ID:
                mSelection = PwsPsalmTable.COLUMN_ID + "=" + uri.getLastPathSegment();
                if (!TextUtils.isEmpty(selection)) mSelection += " AND " + selection;
                break;
            case PATH_PSALM_NUMBERS:
                mSelection = PwsPsalmNumbersTable.COLUMN_PSALMID + "=" + uri.getPathSegments().get(1);
                if (!TextUtils.isEmpty(selection)) mSelection += " AND " + selection;
                if (projection == null || projection.length < 1) {
                    projection = DEFAULT_PSALMNUMBERS_PROJECTION;
                }
                mCursor = mDatabase.query(TABLE_PSALMNUMBERS_JOIN_BOOKS, projection, mSelection, selectionArgs, null, null, sortOrder);
                break;
            case PATH_PSALMS_SUGGESTIONS_NUMBER:
                mLimit = uri.getQueryParameter(SUGGEST_PARAMETER_LIMIT);
                mSelection = PwsPsalmNumbersTable.COLUMN_NUMBER + "=" + uri.getLastPathSegment();
                mCursor = mDatabase.query(TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS, SUGGESTIONS_PSALM_NUMBERS_PROJECTION, mSelection, null, null, null, null, mLimit);
                break;
            case PATH_PSALMS_SUGGESTIONS_NAME:
                mLimit = uri.getQueryParameter(SUGGEST_PARAMETER_LIMIT);
                mSelection = SUGGEST_COLUMN_TEXT_1 + " LIKE '" + uri.getLastPathSegment() + "%'";
                mCursor = mDatabase.query(TABLE_PSALMS, SUGGESTIONS_PSALMS_PROJECTION, mSelection, null, null, null, null, mLimit);
                if (mCursor != null && mCursor.getCount() < 1) {
                    mSelection = SUGGEST_COLUMN_TEXT_1 + " LIKE '% " + uri.getLastPathSegment() + "%'";
                    mCursor = mDatabase.query(TABLE_PSALMS, SUGGESTIONS_PSALMS_PROJECTION, mSelection, null, null, null, null, mLimit);
                }
                break;
            default:
                // todo: throw exception - incorrect uri
        }
        if (mCursor == null) {
            // todo: throw exception
        }
        return mCursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case PATH_PSALMS:
                return "vnd.android.cursor.dir/" + PwsPsalmTable.class.getName();
            case PATH_PSALMS_ID:
                return "vnd.android.cursor.item/" + PwsPsalmTable.class.getName();
            case PATH_PSALM_NUMBERS:
                return "vnd.android.cursor.dir/" + PwsPsalmNumbersTable.class.getName();
            case PATH_PSALM_NUMBERS_ID:
                return "vnd.android.cursor.item/" + PwsPsalmNumbersTable.class.getName();
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
