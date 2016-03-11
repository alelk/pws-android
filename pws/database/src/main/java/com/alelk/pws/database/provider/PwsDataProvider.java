package com.alelk.pws.database.provider;

import static com.alelk.pws.database.provider.PwsDataProviderContract.HISTORY_DATE_FORMAT;
import static com.alelk.pws.database.table.PwsPsalmTable.TABLE_PSALMS;
import static com.alelk.pws.database.table.PwsPsalmNumbersTable.TABLE_PSALMNUMBERS;
import static com.alelk.pws.database.table.PwsBookTable.TABLE_BOOKS;
import static com.alelk.pws.database.table.PwsFavoritesTable.TABLE_FAVORITES;
import static com.alelk.pws.database.table.PwsHistoryTable.TABLE_HISTORY;
import static android.app.SearchManager.*;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.alelk.pws.database.helper.PwsDatabaseHelper;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;
import com.alelk.pws.database.table.PwsPsalmTable;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static com.alelk.pws.database.provider.PwsDataProviderContract.AUTHORITY;
import static com.alelk.pws.database.provider.PwsDataProviderContract.DATABASE_NAME;
import static com.alelk.pws.database.provider.PwsDataProviderContract.DATABASE_VERSION;

/**
 * Created by Alex Elkin on 21.05.2015.
 */
public class PwsDataProvider extends ContentProvider {

    private static final String LOG_TAG = PwsDataProvider.class.getSimpleName();

    // todo: move this constants


    private static final int PATH_PSALMS = 1;
    private static final int PATH_PSALMS_ID = 2;
    private static final int PATH_PSALM_NUMBERS = 3;
    private static final int PATH_PSALM_NUMBERS_ID = 4;
    private static final int PATH_PSALMS_SUGGESTIONS_NUMBER = 5;
    private static final int PATH_PSALMS_SUGGESTIONS_NAME = 6;
    private static final int PATH_FAVORITES = 10;
    private static final int PATH_FAVORITES_ID = 11;
    private static final int PATH_HISTORY = 12;
    private static final int PATH_HISTORY_ID = 13;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS, PATH_PSALMS);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS + "/#", PATH_PSALMS_ID);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS + "/#/" + TABLE_PSALMNUMBERS, PATH_PSALM_NUMBERS);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS + "/#/" + TABLE_PSALMNUMBERS + "/#", PATH_PSALM_NUMBERS_ID);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY + "/#", PATH_PSALMS_SUGGESTIONS_NUMBER);
        sUriMatcher.addURI(AUTHORITY, TABLE_PSALMS +  "/" + SUGGEST_URI_PATH_QUERY + "/*", PATH_PSALMS_SUGGESTIONS_NAME);
        sUriMatcher.addURI(AUTHORITY, TABLE_FAVORITES, PATH_FAVORITES);
        sUriMatcher.addURI(AUTHORITY, TABLE_FAVORITES + "/#", PATH_FAVORITES_ID);
        sUriMatcher.addURI(AUTHORITY, TABLE_HISTORY, PATH_HISTORY);
        sUriMatcher.addURI(AUTHORITY, TABLE_HISTORY + "/#", PATH_HISTORY_ID);
    }

    private static final String TABLE_PSALMNUMBERS_JOIN_BOOKS = TABLE_PSALMNUMBERS + " AS pn " +
            "INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    private static final String TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS = TABLE_PSALMS + " AS p " +
            "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
            "ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID +
            " INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    private static final String TABLE_FAVORITES_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS = TABLE_FAVORITES + " AS fv " +
            "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
            "ON fv." + PwsFavoritesTable.COLUMN_PSALMNUMBERID + "=pn." + PwsPsalmNumbersTable.COLUMN_ID +
            " INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
            " INNER JOIN " + TABLE_PSALMS + " as p " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_PSALMID + "=p." + PwsPsalmTable.COLUMN_ID;

    private static final String TABLE_HISTORY_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS = TABLE_HISTORY + " AS h " +
            "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
            "ON h." + PwsHistoryTable.COLUMN_PSALMNUMBERID + "=pn." + PwsPsalmNumbersTable.COLUMN_ID +
            " INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
            " INNER JOIN " + TABLE_PSALMS + " as p " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_PSALMID + "=p." + PwsPsalmTable.COLUMN_ID;

    /**
     * pn._id AS _id
     * pn.psalmid AS psalmid
     * pn.number AS number
     * pn.bookid AS bookid
     * b.edition AS edition
     * b.displayname AS displayname
     * b.name AS name
     * p.name AS name
     */
    private static final String[] DEFAULT_PSALMNUMBERS_PROJECTION = {
            "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + PwsPsalmNumbersTable.COLUMN_ID,
            "pn." + PwsPsalmNumbersTable.COLUMN_PSALMID + " AS " + PwsPsalmNumbersTable.COLUMN_PSALMID,
            "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + PwsPsalmNumbersTable.COLUMN_NUMBER,
            "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " AS " + PwsPsalmNumbersTable.COLUMN_BOOKID,
            "b." + PwsBookTable.COLUMN_EDITION + " AS " + PwsBookTable.COLUMN_EDITION,
            "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + PwsBookTable.COLUMN_DISPLAYNAME,
            "p." + PwsPsalmTable.COLUMN_NAME + " AS " + PwsPsalmTable.COLUMN_NAME
    };

    private static final String[] SUGGESTIONS_PSALMS_PROJECTION = {
            "psugg." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + PwsPsalmNumbersTable.COLUMN_ID,
            "psugg." + PwsPsalmNumbersTable.COLUMN_PSALMID + " AS " + PwsPsalmNumbersTable.COLUMN_PSALMID,
            "psugg." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + PwsPsalmNumbersTable.COLUMN_NUMBER,
            "psugg." + PwsPsalmNumbersTable.COLUMN_BOOKID + " AS " + PwsPsalmNumbersTable.COLUMN_BOOKID,
            "psugg." + PwsBookTable.COLUMN_EDITION + " AS " + PwsBookTable.COLUMN_EDITION,
            "psugg." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + PwsBookTable.COLUMN_DISPLAYNAME,
            "psugg." + PwsPsalmTable.COLUMN_NAME + " AS " + PwsPsalmTable.COLUMN_NAME,
            "psugg." + PwsPsalmTable.COLUMN_NAME + " AS " + SUGGEST_COLUMN_TEXT_1,
            "psugg." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + SUGGEST_COLUMN_INTENT_DATA_ID
    };

    private static final String[] SUGGESTIONS_PSALM_NUMBERS_PROJECTION = {
            "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + PwsPsalmNumbersTable.COLUMN_ID,
            "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + PwsPsalmNumbersTable.COLUMN_NUMBER,
            "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + SUGGEST_COLUMN_TEXT_2,
            "p." + PwsPsalmTable.COLUMN_NAME + " AS " + SUGGEST_COLUMN_TEXT_1,
            "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + SUGGEST_COLUMN_INTENT_DATA_ID
    };

    /**
     * fv.position as position
     * b.edition as edition
     * pn.number as number
     * p.name as name
     * b.displayname as displayname
     * fv._id as _id
     */
    private static final String[] DEFAULT_FAVORITES_PROJECTION = {
            "fv." + PwsFavoritesTable.COLUMN_POSITION + " AS " + PwsFavoritesTable.COLUMN_POSITION,
            "fv." + PwsFavoritesTable.COLUMN_PSALMNUMBERID + " AS " + PwsFavoritesTable.COLUMN_PSALMNUMBERID,
            "b." + PwsBookTable.COLUMN_EDITION + " AS " + PwsBookTable.COLUMN_EDITION,
            "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + PwsPsalmNumbersTable.COLUMN_NUMBER,
            "p." + PwsPsalmTable.COLUMN_NAME + " AS " + PwsPsalmTable.COLUMN_NAME,
            "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + PwsBookTable.COLUMN_DISPLAYNAME,
            "fv." + PwsFavoritesTable.COLUMN_ID + " AS " + PwsFavoritesTable.COLUMN_ID
    };

    private static final String[] DEFAULT_HISTORY_PROJECTION = {
            "h." + PwsHistoryTable.COLUMN_ID + " AS " + PwsFavoritesTable.COLUMN_ID,
            "h." + PwsHistoryTable.COLUMN_PSALMNUMBERID + " AS " + PwsHistoryTable.COLUMN_PSALMNUMBERID,
            "h." + PwsHistoryTable.COLUMN_ACCESSTIMESTAMP + " AS " + PwsHistoryTable.COLUMN_ACCESSTIMESTAMP,
            "b." + PwsBookTable.COLUMN_EDITION + " AS " + PwsBookTable.COLUMN_EDITION,
            "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + PwsPsalmNumbersTable.COLUMN_NUMBER,
            "p." + PwsPsalmTable.COLUMN_NAME + " AS " + PwsPsalmTable.COLUMN_NAME,
            "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + PwsBookTable.COLUMN_DISPLAYNAME
    };

    private static final String SELECTION_FAVORITE_ID_MATCH = "fv." + PwsFavoritesTable.COLUMN_ID + " = ?";

    private SQLiteDatabase mDatabase;
    private PwsDatabaseHelper mDatabaseHelper;
    private String mSelection;
    private String mLimit;
    private Cursor mCursor;
    SimpleDateFormat mDateFormat = new SimpleDateFormat(HISTORY_DATE_FORMAT);

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
                long psalmId = Long.parseLong(uri.getPathSegments().get(1));
                mCursor = queryPsalmNumbers(psalmId, null, null, null);
                break;
            case PATH_FAVORITES:
                mCursor = queryFavorites(projection, selection, selectionArgs, null, null);
                break;
            case PATH_FAVORITES_ID:
                long id = Long.parseLong(uri.getLastPathSegment());
                mCursor = queryFavorite(id);
                break;
            case PATH_HISTORY:
                mCursor = queryHistory(projection, selection, selectionArgs, null, null);
                break;
            case PATH_PSALMS_SUGGESTIONS_NUMBER:
                mLimit = uri.getQueryParameter(SUGGEST_PARAMETER_LIMIT);
                mSelection = PwsPsalmNumbersTable.COLUMN_NUMBER + "=" + uri.getLastPathSegment();
                mCursor = querySuggestionsPsalmNumber(mSelection, mLimit);
                break;
            case PATH_PSALMS_SUGGESTIONS_NAME:
                mLimit = uri.getQueryParameter(SUGGEST_PARAMETER_LIMIT);
                mSelection = "p." + PwsPsalmTable.COLUMN_NAME + " LIKE '" + uri.getLastPathSegment() + "%'";
                mCursor = querySuggestionsPsalmName(mSelection, mLimit);
                if (mCursor != null && mCursor.getCount() < 1) {
                    mSelection = "p." + PwsPsalmTable.COLUMN_NAME + " LIKE '% " + uri.getLastPathSegment() + "%'";
                    mCursor = querySuggestionsPsalmName(mSelection, mLimit);
                }
                break;
            default:
                // todo: throw exception - incorrect uri
        }
        if (mCursor == null) {
            // todo: throw exception
        }
        mCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return mCursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case PATH_PSALMS:
                return "vnd.android.cursor.dir/" + AUTHORITY + "." + TABLE_PSALMS;
            case PATH_PSALMS_ID:
                return "vnd.android.cursor.item/" + AUTHORITY + "." + TABLE_PSALMS;
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final String METHOD_NAME = "insert";
        Log.v(LOG_TAG, METHOD_NAME + ": uri='" + uri + "'");
        mDatabase = mDatabaseHelper.getWritableDatabase();
        Uri itemUri = null;
        long id;
        switch (sUriMatcher.match(uri)) {
            case PATH_PSALMS:
                id = mDatabase.insert(TABLE_PSALMS, null, values);
                if (id == -1) {
                    // TODO: 24.02.2016 throw exception
                    Log.w(LOG_TAG, METHOD_NAME + ": Error inserting into '" + TABLE_PSALMS + "' table. Uri='" + uri + "'");
                    return null;
                }
                itemUri = ContentUris.withAppendedId(uri, id);
                break;
            case PATH_PSALMS_ID:
                // todo throw exception: cannot insert to row
                break;
            case PATH_FAVORITES:
                id = insertFavorite(values);
                if (id == -1) {
                    // TODO: 24.02.2016 throw exception
                    Log.w(LOG_TAG, METHOD_NAME + ": Error inserting into '" + TABLE_FAVORITES + "' table. Uri='" + uri + "'");
                    return null;
                }
                itemUri = ContentUris.withAppendedId(uri, id);
                break;
            case PATH_HISTORY:
                id = insertHistory(values);
                if (id == -1) {
                    // TODO: 24.02.2016 throw exception
                    Log.w(LOG_TAG, METHOD_NAME + ": Error inserting into '" + TABLE_HISTORY + "' table. Uri='" + uri + "'");
                    return null;
                }
                itemUri = ContentUris.withAppendedId(uri, id);
                break;
            default:
                // TODO: 24.02.2016 throw exception incorrect uri
                Log.w(LOG_TAG, METHOD_NAME + ": Incorrect uri. Uri='" + uri + "'");


        }
        return itemUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final String METHOD_NAME = "delete";
        Log.v(LOG_TAG, METHOD_NAME + ": uri='" + uri.toString() + "'");
        mDatabase = mDatabaseHelper.getReadableDatabase();
        int n = 0;
        switch (sUriMatcher.match(uri)) {
            case PATH_FAVORITES:
                n = deleteFavorites(selection, selectionArgs);
                break;
            default:
                // todo: throw exception - incorrect uri
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return n;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private Cursor queryFavorites(@Nullable String[] projection,
                                  @Nullable String selection,
                                  @Nullable String[] selectionArgs,
                                   @Nullable String orderBy,
                                   @Nullable String limit) {
        if (projection == null) projection = DEFAULT_FAVORITES_PROJECTION;
        if (orderBy == null) orderBy = "fv." + PwsFavoritesTable.COLUMN_POSITION + " DESC";

        Cursor cursor = mDatabase.query(TABLE_FAVORITES_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS,
                projection, selection, selectionArgs,
                "fv." + PwsFavoritesTable.COLUMN_POSITION, null,
                orderBy, limit);
        return cursor;
    }

    private Cursor queryHistory(@Nullable String[] projection,
                                  @Nullable String selection,
                                  @Nullable String[] selectionArgs,
                                  @Nullable String orderBy,
                                  @Nullable String limit) {
        if (projection == null) projection = DEFAULT_HISTORY_PROJECTION;
        if (orderBy == null) orderBy = "h." + PwsHistoryTable.COLUMN_ID + " DESC";

        Cursor cursor = mDatabase.query(TABLE_HISTORY_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_PSALMS,
                projection, selection, selectionArgs,
                "h." + PwsHistoryTable.COLUMN_ID, null,
                orderBy, limit);
        return cursor;
    }

    private Cursor queryAllFavorites(@Nullable String[] projection,
                                      @Nullable String orderBy) {
        return queryFavorites(projection, null, null, orderBy, null);
    }

    private Cursor queryFavorite(long id) {
        String[] selectionArgs = (String[]) Arrays.asList(String.valueOf(id)).toArray();
        return queryFavorites(null, SELECTION_FAVORITE_ID_MATCH, selectionArgs, null, null);
    }

    private Cursor queryLastFavorite(@Nullable String[] projection) {
        return queryFavorites(projection, null, null, null, "1");
    }

    private Cursor querySuggestionsPsalmNumber(@Nullable String selection,
                                               @Nullable String limit) {
        Cursor cursor = mDatabase.query(TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS,
                SUGGESTIONS_PSALM_NUMBERS_PROJECTION,
                selection, null, null, null, null,
                limit);
        return cursor;
    }

    private Cursor querySuggestionsPsalmName(@Nullable String selection, @Nullable String limit) {
        final String METHOD_NAME = "querySuggestionsPsalmName";
        // TODO: 01.03.2016 group by preferred books table
        final String orderBy = "b." + PwsBookTable.COLUMN_ID + " DESC";

        /**
        select * from (
            select pn._id, pn.number, pn.psalmid as psalmid, b.edition, b.displayname, p.name
            from psalmnumbers as pn
            join psalms as p on pn.psalmid=p._id
            join books as b on pn.bookid=b._id
            where p.name LIKE "Name%"
            order by pn.bookid DESC)
        as psugg
        group by psugg.psalmid;
         */
        String rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS,
                DEFAULT_PSALMNUMBERS_PROJECTION,
                selection, null, null,
                orderBy, null);
        final String groupBy = "psugg." + PwsPsalmNumbersTable.COLUMN_PSALMID;
        rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                "(" + rawQuery + ") AS psugg",
                SUGGESTIONS_PSALMS_PROJECTION, null, groupBy, null, null, limit);
        Log.v(LOG_TAG, METHOD_NAME + ": SQLite raw query: " + rawQuery);
        Cursor cursor = mDatabase.rawQuery(rawQuery, null);
        return cursor;
    }

    private Cursor queryPsalmNumbers(long psalmId,
                                     @Nullable String[] projection,
                                     @Nullable String sortOrder,
                                     @Nullable String limit) {
        String selection = PwsPsalmNumbersTable.COLUMN_PSALMID + "=" + psalmId;
        if (projection == null) projection = DEFAULT_PSALMNUMBERS_PROJECTION;

        Cursor cursor = mDatabase.query(TABLE_PSALMNUMBERS_JOIN_BOOKS,
                projection,
                selection, null, null, null,
                sortOrder,
                limit);
        return cursor;
    }

    private Cursor queryPsalmNumberMorePreferred(long psalmId) {
        final String orderBy = "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID;
        // TODO: 01.03.2016 add preferred book selection logic
        return queryPsalmNumbers(psalmId, null, orderBy, "1");
    }

    private long insertFavorite(ContentValues values) {
        final String METHOD_NAME = "insertFavorite";
        Cursor lastFavorite = queryLastFavorite(null);
        long favoritePosition = 1;
        if (lastFavorite.moveToFirst()) {
            favoritePosition = 1 + lastFavorite.getLong(lastFavorite.getColumnIndex(PwsFavoritesTable.COLUMN_POSITION));
        }
        if (values.containsKey(PwsFavoritesTable.COLUMN_POSITION)) {
            long valuePosition = values.getAsLong(PwsFavoritesTable.COLUMN_POSITION);
            if (valuePosition < favoritePosition) {
                // TODO: 29.02.2016 shift favorites list
                Log.w(LOG_TAG, METHOD_NAME + ": Try to insert favorite with position='" +
                        valuePosition + "'. Error inserting: unable to shift favorites list. ");
            }
        }
        values.put(PwsFavoritesTable.COLUMN_POSITION, favoritePosition);
        long id = mDatabase.insert(TABLE_FAVORITES, null, values);
        return id;
    }

    private long insertHistory(ContentValues values) {
        final String METHOD_NAME = "insertHistory";
        if (!values.containsKey(PwsHistoryTable.COLUMN_ACCESSTIMESTAMP)) {
            String timestamp = mDateFormat.format(new Date());
            values.put(PwsHistoryTable.COLUMN_ACCESSTIMESTAMP, timestamp);
        }
        long id = mDatabase.insert(TABLE_HISTORY, null, values);
        return id;
    }

    private int deleteFavorites(String whereClause, String[] whereArgs) {
        return mDatabase.delete(TABLE_FAVORITES, whereClause, whereArgs);
    }
}
