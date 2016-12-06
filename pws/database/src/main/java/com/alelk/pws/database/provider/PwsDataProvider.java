package com.alelk.pws.database.provider;

import static com.alelk.pws.database.table.PwsPsalmTable.TABLE_PSALMS;
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
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.alelk.pws.database.helper.PwsDatabaseHelper;
import com.alelk.pws.database.table.PwsBookStatisticTable;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;
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
        //System.loadLibrary("sqlite");
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
        URI_MATCHER.addURI(AUTHORITY, PsalmNumbers.Book.BookPsalmNumbers.PATH, PsalmNumbers.Book.BookPsalmNumbers.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, PsalmNumbers.Book.BookPsalmNumbers.Info.PATH, PsalmNumbers.Book.BookPsalmNumbers.Info.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH, BookStatistic.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH_ID, BookStatistic.URI_MATCH_ID);
        URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH_TEXT, BookStatistic.URI_MATCH_TEXT);
    }

    private SQLiteDatabase mDatabase;
    private PwsDatabaseHelper mDatabaseHelper;
    private String mSelection;
    private String mLimit;
    private Cursor mCursor;
    SimpleDateFormat mDateFormat = new SimpleDateFormat(HISTORY_TIMESTAMP_FORMAT);

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new PwsDatabaseHelper(getContext());
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
        long psalmNumberId;
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
                //mCursor = queryPsalmNumbers(psalmId, null, null, null);
                break;
            case Favorites.URI_MATCH:
                mCursor = queryFavorites(projection, selection, selectionArgs, null, null);
                break;
            case Favorites.URI_MATCH_ID:
                long id = Long.parseLong(uri.getLastPathSegment());
                mCursor = queryFavorite(id);
                break;
            case History.URI_MATCH:
                mLimit = uri.getQueryParameter(QUERY_PARAMETER_LIMIT);
                mCursor = queryHistory(projection, selection, selectionArgs, null, mLimit);
                break;
            case History.Last.URI_MATCH:
                mCursor = queryHistory(projection, null, null, History.Last.SORT_ORDER, History.Last.LIMIT);
                break;
            case Psalms.Suggestions.URI_MATCH_NUMBER:
                mLimit = uri.getQueryParameter(SUGGEST_PARAMETER_LIMIT);
                mCursor = querySuggestionsPsalmNumber(uri.getLastPathSegment(), mLimit);
                break;
            case Psalms.Suggestions.URI_MATCH_NAME:
                mLimit = uri.getQueryParameter(SUGGEST_PARAMETER_LIMIT);
                //String searchText = uri.getLastPathSegment().trim() + '*';
                String searchText = uri.getLastPathSegment();
                // TODO: 13.11.2016 refactor these statements
                // mCursor = querySuggestionsPsalmName(searchText, mLimit);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                    mCursor = querySuggestionsPsalmNameApi16_2(searchText, mLimit);
                else mCursor = querySuggestionsPsalmNameApi21(searchText, mLimit);
                break;
            case Psalms.Search.URI_MATCH:
                if (selectionArgs == null || selectionArgs.length < 1) {
                    break;
                }
                try {
                    Integer.parseInt(selectionArgs[0]);
                    mCursor = querySearchPsalmNumber(selectionArgs, "50");
                } catch (NumberFormatException ex) {
                    String text = (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)? selectionArgs[0].toLowerCase() : selectionArgs[0];
                    selectionArgs[0] = text.trim().replaceAll("\\s++", "* NEAR/6 ") + "*";
                    mCursor = querySearchPsalmText(selection, selectionArgs, "50");
                }
                break;
            case PsalmNumbers.Psalm.URI_MATCH:
                psalmNumberId = Long.parseLong(uri.getPathSegments().get(1));
                mCursor = queryPsalmNumberPsalm(psalmNumberId, projection, selection, selectionArgs);
                break;
            case PsalmNumbers.Book.BookPsalmNumbers.URI_MATCH:
                psalmNumberId = Long.parseLong(uri.getPathSegments().get(1));
                mCursor = queryPsalmNumberBookPsalmNumbers(psalmNumberId, projection);
                break;
            case PsalmNumbers.Book.BookPsalmNumbers.Info.URI_MATCH:
                psalmNumberId = Long.parseLong(uri.getPathSegments().get(1));
                mCursor = queryPsalmNumberBookPsalmNumberInfo(psalmNumberId, projection);
                break;
            case BookStatistic.URI_MATCH:
                mCursor = queryBookStatistic(null, null, null, null);
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
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            case History.URI_MATCH:
                id = insertHistory(values);
                if (id == -1) {
                    // TODO: 24.02.2016 throw exception
                    Log.w(LOG_TAG, METHOD_NAME + ": Error inserting into '" + TABLE_HISTORY + "' table. Uri='" + uri + "'");
                    return null;
                }
                itemUri = ContentUris.withAppendedId(uri, id);
                getContext().getContentResolver().notifyChange(uri, null);
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
        mDatabase = mDatabaseHelper.getWritableDatabase();
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
        final String METHOD_NAME = "update";
        Log.v(LOG_TAG, METHOD_NAME + ": uri='" + uri.toString() + "'");
        mDatabase = mDatabaseHelper.getWritableDatabase();
        int m = 0;
        switch (URI_MATCHER.match(uri)) {
            case BookStatistic.URI_MATCH_TEXT:
                m = updateBookStatistic(values, uri.getLastPathSegment());
                break;
        }
        return m;
    }


    private Cursor queryFavorites(@Nullable String[] projection,
                                  @Nullable String selection,
                                  @Nullable String[] selectionArgs,
                                   @Nullable String orderBy,
                                   @Nullable String limit) {
        final String METHOD_NAME = "queryFavorites";
        if (projection == null) projection = Favorites.PROJECTION;
        if (orderBy == null) orderBy = Favorites.SORT_ORDER;

        Cursor cursor = mDatabase.query(Favorites.TABLES,
                projection, selection, selectionArgs,
                Favorites.GROUP_BY, null,
                orderBy, limit);
        Log.v(LOG_TAG, METHOD_NAME + ": projection=" + Arrays.toString(projection) +
                " selection='" + selection + "' selectionArgs=" + Arrays.toString(selectionArgs) +
                " orderBy='" + orderBy + "' limit=" + limit +
                " results: " + (cursor == null ? "cursor=null" : cursor.getCount()));
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
        final String METHOD_NAME = "queryLastFavorite";
        final Cursor cursor = queryFavorites(projection, null, null, null, "1");
        Log.v(LOG_TAG, METHOD_NAME + ": projection=" + Arrays.toString(projection) + " results: " + (cursor == null ? "cursor=null" : cursor.getCount()));
        return cursor;
    }

    private Cursor querySuggestionsPsalmNumber(String psalmNumber,
                                               @Nullable String limit) {
        Cursor cursor = mDatabase.query(Psalms.Suggestions.SGNUM_TABLES,
                Psalms.Suggestions.SGNUM_PROJECTION,
                Psalms.Suggestions.getSgNumberSelection(psalmNumber) + " and " +
                        BookStatistic.SELECTION_PREFERRED_BOOKS_ONLY, null, null, null,
                Psalms.Suggestions.SGNUM_SORT_ORDER,
                limit);
        return cursor;
    }

    private Cursor querySuggestionsPsalmNameApi21(@Nullable String searchName, @Nullable String limit) {
        return querySuggestionsPsalmNameApi21(searchName, limit, false);
    }

    private Cursor querySuggestionsPsalmNameApi16(@Nullable String searchName, @Nullable String limit) {
        return querySuggestionsPsalmNameFtsApi16(searchName, limit);
    }

    private Cursor querySuggestionsPsalmNameApi16_2(@Nullable String searchName, @Nullable String limit) {
        return querySuggestionsPsalmNameFtsApi16_2(searchName, limit);
    }

    private Cursor querySuggestionsPsalmNameApi21(@Nullable String searchName, @Nullable String limit, boolean more) {
        final String METHOD_NAME = "querySuggestionsPsalmNameApi21";
        if(TextUtils.isEmpty(limit)) limit = Psalms.Suggestions.Api21.LIMIT;
        mSelection = more ? Psalms.Suggestions.Api21.getSgNameSelectionMore(searchName):
                Psalms.Suggestions.Api21.getSgNameSelection(searchName);

        Cursor cursor = mDatabase.query(Psalms.Suggestions.Api21.TABLES,
                Psalms.Suggestions.Api21.SGNAME_PROJECTION,
                mSelection,
                null, null, null,
                Psalms.Suggestions.Api21.SGNAME_SORT_ORDER,
                limit);
        Log.v(LOG_TAG, METHOD_NAME + ": selection={" + mSelection + "} more=" + more +
                " results:" + (cursor == null? 0 : cursor.getCount()));
        if (!more && (cursor == null || cursor.getCount() < 2)) {
            cursor = querySuggestionsPsalmNameApi21(searchName, limit, true);
        }
        return cursor;
    }

    private Cursor querySuggestionsPsalmNameFts(@Nullable String searchName, @Nullable String limit) {
        final String METHOD_NAME = "querySuggestionsPsalmName";
        String rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                Psalms.Suggestions.SGNAME_TABLES,
                Psalms.Suggestions.SGNAME_RAW1_PROJECTION,
                Psalms.Suggestions.getSgNameSelection(searchName) + " and " +
                        Psalms.Suggestions.SELECTION_PREFERRED_BOOKS_ONLY, null, null,
                Psalms.Suggestions.SGNAME_RAW1_ORDERBY, null);
        rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                "(" + rawQuery + ") AS " + Psalms.Suggestions.SGNAME_TABLE,
                Psalms.Suggestions.SGNAME_PROJECTION, null,
                Psalms.Suggestions.SGNAME_RAW2_GROUPBY, null, null, limit);
        Log.v(LOG_TAG, METHOD_NAME + ": SQLite raw query: " + rawQuery);
        return mDatabase.rawQuery(rawQuery, null);
    }

    private Cursor querySuggestionsPsalmNameFtsApi16(@Nullable String searchName, @Nullable String limit) {
        Cursor cursor =  mDatabase.query(Psalms.Suggestions.Api16.TABLES,
                Psalms.Suggestions.Api16.PROJECTION,
                Psalms.Suggestions.Api16.getSelection(searchName.toLowerCase()), null, Psalms.Suggestions.Api16.GROUPBY,
                null, null, limit);
        Log.i(LOG_TAG, " size = " + cursor.getCount() + cursor);
        return cursor;
    }

    private Cursor querySuggestionsPsalmNameFtsApi16_2(@Nullable String searchName, @Nullable String limit) {
        Cursor cursor =  mDatabase.query(Psalms.Suggestions.Api16_2.TABLES,
                Psalms.Suggestions.Api16_2.PROJECTION,
                Psalms.Suggestions.Api16_2.getSelection(searchName.toLowerCase()), null, Psalms.Suggestions.Api16_2.GROUPBY,
                null, null, limit);
        Log.i(LOG_TAG, " size = " + cursor.getCount() + cursor);
        return cursor;
    }

    private Cursor querySearchPsalmNumber(@Nullable String[] selectionArgs,
                                          @Nullable String limit) {
        return mDatabase.query(Psalms.Search.SNUM_TABLES,
                Psalms.Search.SNUM_PROJECTION,
                Psalms.Search.SNUM_SELECTION + " and " +
                        BookStatistic.SELECTION_PREFERRED_BOOKS_ONLY,
                selectionArgs, null, null,
                Psalms.Search.SNUM_ORDER_BY,
                limit);
    }

    private Cursor querySearchPsalmText(@Nullable String selection,
                                        @Nullable String[] selectionArgs,
                                        @Nullable String limit) {
        final String METHOD_NAME = "querySearchPsalmText";
        String rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                Psalms.Search.STXT_RAW1_TABLES,
                Psalms.Search.STXT_RAW1_PROJECTION,
                selection + " and " + Psalms.Search.STXT_RAW1_SELECTION_PREFERRED_BOOKS_ONLY,
                null, null,
                Psalms.Search.STXT_RAW1_ORDER_BY,
                limit);
        rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                "(" + rawQuery + ") AS search",
                Psalms.Search.STXT_PROJECTION, null, Psalms.Search.STXT_RAW2_GROUP_BY, null, Psalms.Search.STXT_RAW2_ORDER_BY, limit);
        Cursor cursor = mDatabase.rawQuery(rawQuery, selectionArgs);
        Log.d(LOG_TAG, METHOD_NAME + ": rawQuery='" + rawQuery + "' selectionArgs=" +
                Arrays.toString(selectionArgs) + " results:" + (cursor == null ? 0 : cursor.getCount()));
        return cursor;
    }

    /*
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
    } */

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

    private Cursor queryPsalmNumberBookPsalmNumbers(long psalmNumberId, @Nullable String[] projection){
        return queryPsalmNumberBookPsalmNumber(psalmNumberId, projection, null, null);
    }

    private Cursor queryPsalmNumberBookPsalmNumber(long psalmNumberId,
                                         @Nullable String[] projection,
                                         @Nullable String selection,
                                         @Nullable String[] selectionArgs) {
        final String METHOD_NAME = "queryPsalmNumberBookPsalmNumber";

        if (projection == null) projection = PsalmNumbers.Book.BookPsalmNumbers.PROJECTION;
        if (selection == null) selectionArgs = null;

        final String rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                PsalmNumbers.Book.BookPsalmNumbers.buildRawTables(psalmNumberId),
                projection, null, null, null,
                PsalmNumbers.Book.BookPsalmNumbers.ORDER_BY, null);
        Cursor cursor = mDatabase.rawQuery(rawQuery, selectionArgs);
        Log.v(LOG_TAG, METHOD_NAME + ": rawQuery='" + rawQuery + "' selectionArgs=" +
                Arrays.toString(selectionArgs) + " results:" + (cursor == null ? 0 : cursor.getCount()));
        return cursor;
    }

    private Cursor queryPsalmNumberBookPsalmNumberInfo(long psalmNumberId, String[] projection) {
        final String METHOD_NAME = "queryPsalmNumberBookPsalmNumberInfo";

        if (projection == null) projection = PsalmNumbers.Book.BookPsalmNumbers.Info.PROJECTION_PSALMNUMBER_ID;

        final String rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                PsalmNumbers.Book.BookPsalmNumbers.Info.buildRawTables(psalmNumberId),
                projection, null, null, null,
                null, null);
        Cursor cursor = mDatabase.rawQuery(rawQuery, null);
        Log.v(LOG_TAG, METHOD_NAME + ": rawQuery='" + rawQuery + " results:" + (cursor == null ? 0 : cursor.getCount()));
        return cursor;
    }

    private Cursor queryBookStatistic(@Nullable String[] projection,
                                @Nullable String selection,
                                @Nullable String[] selectionArgs,
                                @Nullable String orderBy) {
        if (projection == null) projection = BookStatistic.PROJECTION;
        if (orderBy == null) orderBy = BookStatistic.SORT_ORDER;

        Cursor cursor = mDatabase.query(BookStatistic.TABLES,
                projection, selection, selectionArgs,
                null, null,
                orderBy, null);
        return cursor;
    }

    /*
    private Cursor queryPsalmNumberMorePreferred(long psalmId) {
        final String orderBy = "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID;
        // TODO: 01.03.2016 add preferred book selection logic
        return queryPsalmNumbers(psalmId, null, orderBy, "1");
    } */

    private long insertFavorite(ContentValues values) {
        final String METHOD_NAME = "insertFavorite";
        Cursor lastFavorite = queryLastFavorite(null);
        long favoritePosition = 2;
        if (lastFavorite != null && lastFavorite.moveToFirst()) {
            favoritePosition = 1 + lastFavorite.getLong(lastFavorite.getColumnIndex(Favorites.COLUMN_FAVORITEPOSITION));
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
        long id=0;
        try {
            id = mDatabase.insert(TABLE_FAVORITES, null, values);
        } finally {
            Log.v(LOG_TAG, METHOD_NAME + ": resultId=" + id + " " +
                    "values=[keySet=" + Arrays.toString(values.keySet().toArray()) +
                    " valueSet=" + Arrays.toString(values.valueSet().toArray()) + "]");
        }
        return id;
    }

    private long insertHistory(ContentValues values) {
        final String METHOD_NAME = "insertHistory";
        if (!values.containsKey(History.COLUMN_HISTORYTIMESTAMP)) {
            String timestamp = mDateFormat.format(new Date());
            values.put(PwsHistoryTable.COLUMN_ACCESSTIMESTAMP, timestamp);
        }
        long id = 0;
        try {
            id = mDatabase.insert(TABLE_HISTORY, null, values);
        } finally {
            Log.v(LOG_TAG, METHOD_NAME + ": resultId=" + id + " " +
                    "values=[keySet=" + Arrays.toString(values.keySet().toArray()) +
                    " valueSet=" + Arrays.toString(values.valueSet().toArray()) + "]");
        }
        return id;
    }

    private int deleteFavorites(String whereClause, String[] whereArgs) {
        return mDatabase.delete(TABLE_FAVORITES, whereClause, whereArgs);
    }

    private int updateBookStatistic(ContentValues values, String bookEdition){
        final String rawSelection = SQLiteQueryBuilder.buildQueryString(false,
                BookStatistic.RAW_TABLES,
                BookStatistic.RAW_PROJECTION,
                BookStatistic.getRawSelection(bookEdition), null, null, null, null);
        return mDatabase.update(PwsBookStatisticTable.TABLE_BOOKSTATISTIC,
                values, "_id=(" + rawSelection + ")",
                null);
    }
}
