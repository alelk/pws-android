package com.alelk.pws.database.provider;

import static com.alelk.pws.database.table.PwsPsalmTable.TABLE_PSALMS;
import static com.alelk.pws.database.table.PwsPsalmFtsTable.TABLE_PSALMS_FTS;
import static com.alelk.pws.database.table.PwsPsalmNumbersTable.TABLE_PSALMNUMBERS;
import static com.alelk.pws.database.table.PwsBookTable.TABLE_BOOKS;
import static com.alelk.pws.database.table.PwsFavoritesTable.TABLE_FAVORITES;
import static com.alelk.pws.database.table.PwsHistoryTable.TABLE_HISTORY;
import static com.alelk.pws.database.table.PwsBookStatisticTable.TABLE_BOOKSTATISTIC;
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
import com.alelk.pws.database.table.PwsBookStatisticTable;
import com.alelk.pws.database.table.PwsBookTable;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.table.PwsPsalmNumbersTable;
import com.alelk.pws.database.table.PwsPsalmTable;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Alex Elkin on 21.05.2015.
 */
public class PwsDataProvider extends ContentProvider implements PwsDataProviderContract {

    private static final String LOG_TAG = PwsDataProvider.class.getSimpleName();

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI(AUTHORITY, Psalms.PATH, Psalms.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, Psalms.PATH_ID, Psalms.URI_MATCH_ID);
        URI_MATCHER.addURI(AUTHORITY, Psalms.PsalmNumbers.PATH, Psalms.PsalmNumbers.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, Psalms.PsalmNumbers.PATH_ID, Psalms.PsalmNumbers.URI_MATCH_ID);
        URI_MATCHER.addURI(AUTHORITY, Psalms.Suggestions.PATH_NUMBER, Psalms.Suggestions.URI_MATCH_NUMBER);
        URI_MATCHER.addURI(AUTHORITY, Psalms.Suggestions.PATH_NAME, Psalms.Suggestions.URI_MATCH_NAME);
        URI_MATCHER.addURI(AUTHORITY, Psalms.Search.PATH, Psalms.Search.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, Favorites.PATH, Favorites.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, Favorites.PATH_ID, Favorites.URI_MATCH_ID);
        URI_MATCHER.addURI(AUTHORITY, History.PATH, History.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, History.PATH_ID, History.URI_MATCH_ID);
        URI_MATCHER.addURI(AUTHORITY, History.Last.PATH, History.Last.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, PsalmNumbers.PATH, PsalmNumbers.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, PsalmNumbers.PATH_ID, PsalmNumbers.URI_MATCH_ID);
        URI_MATCHER.addURI(AUTHORITY, PsalmNumbers.Psalm.PATH, PsalmNumbers.Psalm.URI_MATCH);
    }

    private static final String TABLE_PSALMNUMBERS_JOIN_BOOKS = TABLE_PSALMNUMBERS + " AS pn " +
            "INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    private static final String TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC =
            TABLE_PSALMS + " AS p " +
            "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
            "ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID +
            " INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
            " INNER JOIN " + TABLE_BOOKSTATISTIC + " as bs " +
            "ON bs." + PwsBookStatisticTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    private static final String TABLE_PSALMS_JOIN_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC =
            TABLE_PSALMS + " AS p " +
            "INNER JOIN " + TABLE_PSALMNUMBERS + " AS pn " +
            "ON p." + PwsPsalmTable.COLUMN_ID + "=pn." + PwsPsalmNumbersTable.COLUMN_PSALMID +
            " INNER JOIN " + TABLE_PSALMS_FTS +
            " ON docid=p." + PwsPsalmTable.COLUMN_ID +
            " INNER JOIN " + TABLE_BOOKS + " as b " +
            "ON pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID +
            " INNER JOIN " + TABLE_BOOKSTATISTIC + " as bs " +
            "ON bs." + PwsBookStatisticTable.COLUMN_BOOKID + "=b." + PwsBookTable.COLUMN_ID;

    private static final String[] DEFAULT_PSALMNUMBERS_PROJECTION = {
            "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + PwsPsalmNumbersTable.COLUMN_ID,
            "pn." + PwsPsalmNumbersTable.COLUMN_PSALMID + " AS " + PwsPsalmNumbersTable.COLUMN_PSALMID,
            "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + PwsPsalmNumbersTable.COLUMN_NUMBER,
            "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " AS " + PwsPsalmNumbersTable.COLUMN_BOOKID,
            "b." + PwsBookTable.COLUMN_EDITION + " AS " + PwsBookTable.COLUMN_EDITION,
            "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " AS " + PwsBookStatisticTable.COLUMN_USERPREFERENCE ,
            "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + PwsBookTable.COLUMN_DISPLAYNAME,
            "p." + PwsPsalmTable.COLUMN_NAME + " AS " + PwsPsalmTable.COLUMN_NAME
    };

    private static final String[] DEFAULT_PSALMNUMBERS_FTS_PROJECTION = {
            "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + PwsPsalmNumbersTable.COLUMN_ID,
            "pn." + PwsPsalmNumbersTable.COLUMN_PSALMID + " AS " + PwsPsalmNumbersTable.COLUMN_PSALMID,
            "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + PwsPsalmNumbersTable.COLUMN_NUMBER,
            "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID + " AS " + PwsPsalmNumbersTable.COLUMN_BOOKID,
            "b." + PwsBookTable.COLUMN_EDITION + " AS " + PwsBookTable.COLUMN_EDITION,
            "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + PwsBookTable.COLUMN_DISPLAYNAME,
            "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " AS " + PwsBookStatisticTable.COLUMN_USERPREFERENCE ,
            "p." + PwsPsalmTable.COLUMN_NAME + " AS " + PwsPsalmTable.COLUMN_NAME,
            "snippet(" + TABLE_PSALMS_FTS + ") as snippet"
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

    private static final String[] SEARCH_PSALMS_PROJECTION = {
            "s." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + PwsPsalmNumbersTable.COLUMN_ID,
            "s." + PwsPsalmNumbersTable.COLUMN_PSALMID + " AS " + PwsPsalmNumbersTable.COLUMN_PSALMID,
            "s." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + PwsPsalmNumbersTable.COLUMN_NUMBER,
            "s." + PwsPsalmNumbersTable.COLUMN_BOOKID + " AS " + PwsPsalmNumbersTable.COLUMN_BOOKID,
            "s." + PwsBookTable.COLUMN_EDITION + " AS " + PwsBookTable.COLUMN_EDITION,
            "s." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + PwsBookTable.COLUMN_DISPLAYNAME,
            "s." + PwsPsalmTable.COLUMN_NAME + " AS " + PwsPsalmTable.COLUMN_NAME,
            "s.snippet AS snippet"
    };

    private static final String[] SUGGESTIONS_PSALM_NUMBERS_PROJECTION = {
            "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + PwsPsalmNumbersTable.COLUMN_ID,
            "pn." + PwsPsalmNumbersTable.COLUMN_NUMBER + " AS " + PwsPsalmNumbersTable.COLUMN_NUMBER,
            "b." + PwsBookTable.COLUMN_DISPLAYNAME + " AS " + SUGGEST_COLUMN_TEXT_2,
            "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + " AS " + PwsBookStatisticTable.COLUMN_USERPREFERENCE ,
            "p." + PwsPsalmTable.COLUMN_NAME + " AS " + SUGGEST_COLUMN_TEXT_1,
            "pn." + PwsPsalmNumbersTable.COLUMN_ID + " AS " + SUGGEST_COLUMN_INTENT_DATA_ID
    };

    private static final String SELECTION_PREFFERED_BOOKS_ONLY = "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE + ">0";

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
        switch (URI_MATCHER.match(uri)) {
            case Psalms.URI_MATCH:
                mCursor = mDatabase.query(TABLE_PSALMS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case Psalms.URI_MATCH_ID:
                mSelection = PwsPsalmTable.COLUMN_ID + "=" + uri.getLastPathSegment();
                if (!TextUtils.isEmpty(selection)) mSelection += " AND " + selection;
                break;
            case Psalms.PsalmNumbers.URI_MATCH:
                long psalmId = Long.parseLong(uri.getPathSegments().get(1));
                mCursor = queryPsalmNumbers(psalmId, null, null, null);
                break;
            case Favorites.URI_MATCH:
                mCursor = queryFavorites(projection, selection, selectionArgs, null, null);
                break;
            case Favorites.URI_MATCH_ID:
                long id = Long.parseLong(uri.getLastPathSegment());
                mCursor = queryFavorite(id);
                break;
            case History.URI_MATCH:
                mCursor = queryHistory(projection, selection, selectionArgs, null, null);
                break;
            case History.Last.URI_MATCH:
                mCursor = queryHistory(projection, null, null, History.Last.SORT_ORDER, History.Last.LIMIT);
                break;
            case Psalms.Suggestions.URI_MATCH_NUMBER:
                mLimit = uri.getQueryParameter(SUGGEST_PARAMETER_LIMIT);
                mSelection = PwsPsalmNumbersTable.COLUMN_NUMBER + "=" + uri.getLastPathSegment();
                mCursor = querySuggestionsPsalmNumber(mSelection, mLimit);
                break;
            case Psalms.Suggestions.URI_MATCH_NAME:
                mLimit = uri.getQueryParameter(SUGGEST_PARAMETER_LIMIT);
                mSelection = "p." + PwsPsalmTable.COLUMN_NAME + " LIKE '" + uri.getLastPathSegment() + "%'";
                mCursor = querySuggestionsPsalmName(mSelection, mLimit);
                if (mCursor != null && mCursor.getCount() < 1) {
                    mSelection = "p." + PwsPsalmTable.COLUMN_NAME + " LIKE '% " + uri.getLastPathSegment() + "%'";
                    mCursor = querySuggestionsPsalmName(mSelection, mLimit);
                }
                break;
            case Psalms.Search.URI_MATCH:
                if (selection != null) {
                    mSelection = selection;
                }
                mCursor = querySearchPsalmText(mSelection, "50");
                break;
            case PsalmNumbers.Psalm.URI_MATCH:
                long psalmNumberId = Long.parseLong(uri.getPathSegments().get(1));
                mCursor = queryPsalmNumberPsalm(psalmNumberId, projection, selection, selectionArgs);
                break;
            default:
                // todo: throw exception - incorrect uri
        }
        if (mCursor == null) {
            // todo: throw exception
            return null;
        }
        mCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return mCursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case Psalms.URI_MATCH:
                return "vnd.android.cursor.dir/" + AUTHORITY + "." + Psalms.PATH;
            case Psalms.URI_MATCH_ID:
                return "vnd.android.cursor.item/" + AUTHORITY + "." + Psalms.PATH;
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
        switch (URI_MATCHER.match(uri)) {
            case Psalms.URI_MATCH:
                id = mDatabase.insert(TABLE_PSALMS, null, values);
                if (id == -1) {
                    // TODO: 24.02.2016 throw exception
                    Log.w(LOG_TAG, METHOD_NAME + ": Error inserting into '" + TABLE_PSALMS + "' table. Uri='" + uri + "'");
                    return null;
                }
                itemUri = ContentUris.withAppendedId(uri, id);
                break;
            case Psalms.URI_MATCH_ID:
                // todo throw exception: cannot insert to row
                break;
            case Favorites.URI_MATCH:
                id = insertFavorite(values);
                if (id == -1) {
                    // TODO: 24.02.2016 throw exception
                    Log.w(LOG_TAG, METHOD_NAME + ": Error inserting into '" + TABLE_FAVORITES + "' table. Uri='" + uri + "'");
                    return null;
                }
                itemUri = ContentUris.withAppendedId(uri, id);
                break;
            case History.URI_MATCH:
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
        switch (URI_MATCHER.match(uri)) {
            case Favorites.URI_MATCH:
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
        if (projection == null) projection = Favorites.PROJECTION;
        if (orderBy == null) orderBy = Favorites.SORT_ORDER;

        Cursor cursor = mDatabase.query(Favorites.TABLES,
                projection, selection, selectionArgs,
                Favorites.GROUP_BY, null,
                orderBy, limit);
        return cursor;
    }

    private Cursor queryHistory(@Nullable String[] projection,
                                  @Nullable String selection,
                                  @Nullable String[] selectionArgs,
                                  @Nullable String orderBy,
                                  @Nullable String limit) {
        if (projection == null) projection = History.PROJECTION;
        if (orderBy == null) orderBy = History.SORT_ORDER;

        Cursor cursor = mDatabase.query(History.TABLES,
                projection, selection, selectionArgs,
                History.GROUP_BY, null,
                orderBy, limit);
        return cursor;
    }

    private Cursor queryAllFavorites(@Nullable String[] projection,
                                      @Nullable String orderBy) {
        return queryFavorites(projection, null, null, orderBy, null);
    }

    private Cursor queryFavorite(long id) {
        String[] selectionArgs = (String[]) Arrays.asList(String.valueOf(id)).toArray();
        return queryFavorites(null, Favorites.SELECTION_ID_MATCH, selectionArgs, null, null);
    }

    private Cursor queryLastFavorite(@Nullable String[] projection) {
        return queryFavorites(projection, null, null, null, "1");
    }

    private Cursor querySuggestionsPsalmNumber(@Nullable String selection,
                                               @Nullable String limit) {
        Cursor cursor = mDatabase.query(TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC,
                SUGGESTIONS_PSALM_NUMBERS_PROJECTION,
                selection + " and " + SELECTION_PREFFERED_BOOKS_ONLY, null, null, null, null,
                limit);
        return cursor;
    }

    private Cursor querySuggestionsPsalmName(@Nullable String selection, @Nullable String limit) {
        final String METHOD_NAME = "querySuggestionsPsalmName";
        final String orderBy = "b." + PwsBookTable.COLUMN_ID + " DESC";
        String rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                TABLE_PSALMS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC,
                DEFAULT_PSALMNUMBERS_PROJECTION,
                selection + " and " + SELECTION_PREFFERED_BOOKS_ONLY, null, null,
                orderBy, null);
        final String groupBy = "psugg." + PwsPsalmNumbersTable.COLUMN_PSALMID;
        rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                "(" + rawQuery + ") AS psugg",
                SUGGESTIONS_PSALMS_PROJECTION, null, groupBy, null, null, limit);
        Log.v(LOG_TAG, METHOD_NAME + ": SQLite raw query: " + rawQuery);
        Cursor cursor = mDatabase.rawQuery(rawQuery, null);
        return cursor;
    }

    private Cursor querySearchPsalmText(@Nullable String selection, @Nullable String limit) {
        final String METHOD_NAME = "querySearchPsalmText";
        // TODO: 01.03.2016 group by preferred books table
        final String orderBy = "bs." + PwsBookStatisticTable.COLUMN_USERPREFERENCE;

        String rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                TABLE_PSALMS_JOIN_PSALMS_FTS_JOIN_PSALMNUMBERS_JOIN_BOOKS_JOIN_BOOKSTATISTIC,
                DEFAULT_PSALMNUMBERS_FTS_PROJECTION,
                selection + " and " + SELECTION_PREFFERED_BOOKS_ONLY, null, null,
                orderBy, null);
        final String groupBy = "s." + PwsPsalmNumbersTable.COLUMN_PSALMID;
        rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                "(" + rawQuery + ") AS s",
                SEARCH_PSALMS_PROJECTION, null, groupBy, null, null, limit);
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
    private Cursor queryPsalmNumberPsalm(long psalmNumberId,
                                         @Nullable String[] projection,
                                         @Nullable String selection,
                                         @Nullable String[] selectionArgs) {
        if (projection == null) projection = PsalmNumbers.Psalm.PROJECTION;
        if (selection == null) {
            selection = PsalmNumbers.Psalm.DEFAULT_SELECTION;
            selectionArgs = new String[] {Long.toString(psalmNumberId)};
        }
        Cursor cursor = mDatabase.query(PsalmNumbers.Psalm.TABLES,
                projection,
                selection, selectionArgs, null, null,
                null);
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
        if (values.containsKey(Favorites.COLUMN_FAVORITEPOSITION)) {
            long valuePosition = values.getAsLong(Favorites.COLUMN_FAVORITEPOSITION);
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
        if (!values.containsKey(History.COLUMN_HISTORYTIMESTAMP)) {
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
