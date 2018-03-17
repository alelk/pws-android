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

package com.alelk.pws.database.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.alelk.pws.database.helper.PwsDatabaseHelper;
import com.alelk.pws.database.table.PwsBookStatisticTable;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static android.app.SearchManager.SUGGEST_PARAMETER_LIMIT;
import static com.alelk.pws.database.table.PwsFavoritesTable.TABLE_FAVORITES;
import static com.alelk.pws.database.table.PwsHistoryTable.TABLE_HISTORY;
import static com.alelk.pws.database.table.PwsPsalmTable.TABLE_PSALMS;

/**
 * Pws data provider
 *
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
        URI_MATCHER.addURI(AUTHORITY, PsalmNumbers.Book.BookPsalmNumbers.PATH, PsalmNumbers.Book.BookPsalmNumbers.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, PsalmNumbers.Book.BookPsalmNumbers.Info.PATH, PsalmNumbers.Book.BookPsalmNumbers.Info.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, PsalmNumbers.ReferencePsalms.PATH, PsalmNumbers.ReferencePsalms.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH, BookStatistic.URI_MATCH);
        URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH_ID, BookStatistic.URI_MATCH_ID);
        URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH_TEXT, BookStatistic.URI_MATCH_TEXT);
    }

    private Context mContext;
    private SQLiteDatabase mDatabase;
    private PwsDatabaseHelper mDatabaseHelper;
    SimpleDateFormat mDateFormat = new SimpleDateFormat(HISTORY_TIMESTAMP_FORMAT, Locale.ENGLISH);

    @Override
    public boolean onCreate() {
        mContext = getContext();
        if (mContext == null) return false;
        mDatabaseHelper = new PwsDatabaseHelper(mContext);
        return true;
    }

    @Override
    public Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        final String METHOD_NAME = "query";
        mDatabase = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = null;
        long psalmNumberId;
        switch (URI_MATCHER.match(uri)) {
            case Psalms.URI_MATCH:
                cursor = mDatabase.query(TABLE_PSALMS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case Psalms.URI_MATCH_ID:
                break;
            case Psalms.PsalmNumbers.URI_MATCH:
                break;
            case Favorites.URI_MATCH:
                cursor = queryFavorites(projection, selection, selectionArgs, null, null);
                break;
            case Favorites.URI_MATCH_ID:
                cursor = queryFavorite(Long.parseLong(uri.getLastPathSegment()));
                break;
            case History.URI_MATCH:
                cursor = queryHistory(projection, selection, selectionArgs, null, uri.getQueryParameter(QUERY_PARAMETER_LIMIT));
                break;
            case History.Last.URI_MATCH:
                cursor = queryHistory(projection, null, null, History.Last.SORT_ORDER, History.Last.LIMIT);
                break;
            case Psalms.Suggestions.URI_MATCH_NUMBER:
                cursor = querySuggestionsPsalmNumber(uri.getLastPathSegment(), uri.getQueryParameter(SUGGEST_PARAMETER_LIMIT));
                break;
            case Psalms.Suggestions.URI_MATCH_NAME:
                String searchText = uri.getLastPathSegment();
                if (searchText == null) break;
                cursor = querySuggestionsPsalmName(searchText, uri.getQueryParameter(SUGGEST_PARAMETER_LIMIT));
                if (cursor == null || cursor.getCount() < 1)
                    cursor = querySuggestionsPsalmText(searchText, uri.getQueryParameter(SUGGEST_PARAMETER_LIMIT));
                break;
            case Psalms.Search.URI_MATCH:
                if (selectionArgs == null || selectionArgs.length < 1) {
                    break;
                }
                try {
                    int num = Integer.parseInt(selectionArgs[0]);
                    cursor = querySearchPsalmNumber(num, "50");
                } catch (NumberFormatException ex) {
                    String text = (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)? selectionArgs[0].toLowerCase() : selectionArgs[0];
                    text = text.trim().replaceAll("\\s++", "* NEAR/6 ") + "*";
                    cursor = querySearchPsalmText(text, "50");
                }
                break;
            case PsalmNumbers.Psalm.URI_MATCH:
                psalmNumberId = Long.parseLong(uri.getPathSegments().get(1));
                cursor = queryPsalmNumberPsalm(psalmNumberId, projection, selection, selectionArgs);
                break;
            case PsalmNumbers.Book.BookPsalmNumbers.URI_MATCH:
                psalmNumberId = Long.parseLong(uri.getPathSegments().get(1));
                cursor = queryPsalmNumberBookPsalmNumbers(psalmNumberId, projection);
                break;
            case PsalmNumbers.Book.BookPsalmNumbers.Info.URI_MATCH:
                psalmNumberId = Long.parseLong(uri.getPathSegments().get(1));
                cursor = queryPsalmNumberBookPsalmNumberInfo(psalmNumberId, projection);
                break;
            case PsalmNumbers.ReferencePsalms.URI_MATCH:
                psalmNumberId = Long.parseLong(uri.getPathSegments().get(1));
                cursor = queryPsalmNumberReferredPsalms(psalmNumberId);
                break;
            case BookStatistic.URI_MATCH:
                cursor = queryBookStatistic(null, null, null, null);
                break;
            default:
                Log.w(LOG_TAG, METHOD_NAME + ": Incorrect uri: '" + uri + '\'');
        }
        if (cursor == null) {
            Log.d(LOG_TAG, METHOD_NAME + ": No results for uri: '" + uri + '\'');
            return null;
        }
        Log.v(LOG_TAG, METHOD_NAME + ": Query for uri='" + uri.toString() + "'. Results: " + cursor.getCount());
        cursor.setNotificationUri(mContext.getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case Psalms.URI_MATCH:
                return "vnd.android.cursor.dir/" + AUTHORITY + "." + Psalms.PATH;
            case Psalms.URI_MATCH_ID:
                return "vnd.android.cursor.item/" + AUTHORITY + "." + Psalms.PATH;
        }
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
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
                mContext.getContentResolver().notifyChange(uri, null);
                break;
            case History.URI_MATCH:
                id = insertHistory(values);
                if (id == -1) {
                    // TODO: 24.02.2016 throw exception
                    Log.w(LOG_TAG, METHOD_NAME + ": Error inserting into '" + TABLE_HISTORY + "' table. Uri='" + uri + "'");
                    return null;
                }
                itemUri = ContentUris.withAppendedId(uri, id);
                mContext.getContentResolver().notifyChange(uri, null);
                break;
            default:
                // TODO: 24.02.2016 throw exception incorrect uri
                Log.w(LOG_TAG, METHOD_NAME + ": Incorrect uri. Uri='" + uri + "'");


        }
        return itemUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final String METHOD_NAME = "delete";
        Log.v(LOG_TAG, METHOD_NAME + ": uri='" + uri.toString() + "'");
        mDatabase = mDatabaseHelper.getWritableDatabase();
        int n = 0;
        switch (URI_MATCHER.match(uri)) {
            case Favorites.URI_MATCH:
                n = deleteFavorites(selection, selectionArgs);
                break;
            case History.URI_MATCH:
                n = deleteHistory(selection, selectionArgs);
            default:
                // todo: throw exception - incorrect uri
        }
        mContext.getContentResolver().notifyChange(uri, null);
        return n;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
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

        return mDatabase.query(History.TABLES,
                projection, selection, selectionArgs,
                History.GROUP_BY, null,
                orderBy, limit);
    }

    private Cursor queryFavorite(long id) {
        String[] selectionArgs = new String[]{String.valueOf(id)};
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
        return  mDatabase.query(Psalms.Suggestions.SGNUM_TABLES,
                Psalms.Suggestions.SGNUM_PROJECTION,
                Psalms.Suggestions.getSgNumberSelection(psalmNumber), null, Psalms.Suggestions.COLUMN_PSALMID, null,
                Psalms.Suggestions.SGNUM_SORT_ORDER,
                limit);
    }

    private Cursor querySuggestionsPsalmName(@NonNull String searchName, @Nullable String limit) {
        return mDatabase.query(Psalms.Suggestions.SG_NAME_TABLES,
                Psalms.Suggestions.SG_NAME_PROJECTION,
                Psalms.Suggestions.getSgNameSelection(searchName.toLowerCase()), null, Psalms.Suggestions.SG_NAME_GROUPBY,
                null, null, limit);
    }

    private Cursor querySuggestionsPsalmText(@NonNull String searchName, @Nullable String limit) {
        return mDatabase.query(Psalms.Suggestions.SG_TXT_TABLES,
                Psalms.Suggestions.SG_TXT_PROJECTION,
                Psalms.Suggestions.getSgTextSelection(searchName.toLowerCase()), null, Psalms.Suggestions.SG_TXT_GROUPBY,
                null, null, limit);
    }

    private Cursor querySearchPsalmNumber(int psalmNumber,
                                          @Nullable String limit) {
        return mDatabase.query(Psalms.Search.S_NUM_TABLES,
                Psalms.Search.S_NUM_PROJECTION,
                Psalms.Search.getSNumSelection(psalmNumber),
                null, null, null,
                Psalms.Search.S_NUM_ORDER_BY,
                limit);
    }

    private Cursor querySearchPsalmText(@NonNull String searchText,
                                        @Nullable String limit) {
        final String METHOD_NAME = "querySearchPsalmText";
        Cursor cursor = mDatabase.query(Psalms.Search.S_TXT_TABLES,
                Psalms.Search.S_TXT_PROJECTION, Psalms.Search.getSTxtSelection(searchText), null,
                null, null,
                Psalms.Search.S_TXT_ORDER_BY, limit);
        Log.d(LOG_TAG, METHOD_NAME + ": searchQuery='" + searchText + "' results:" + (cursor == null ? 0 : cursor.getCount()));
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
        return mDatabase.query(PsalmNumbers.Psalm.TABLES,
                projection,
                selection, selectionArgs, null, null,
                null);
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

    private Cursor queryPsalmNumberReferredPsalms(long currentPsalmNumberId) {
        final String METHOD_NAME = "queryPsalmNumberReferredPsalms";

        final String rawQuery = SQLiteQueryBuilder.buildQueryString(false,
                PsalmNumbers.ReferencePsalms.buildRawTables(currentPsalmNumberId),
                PsalmNumbers.ReferencePsalms.PROJECTION, null,
                PsalmNumbers.ReferencePsalms.COLUMN_PSALM_ID, null,
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

        return mDatabase.query(BookStatistic.TABLES,
                projection, selection, selectionArgs,
                null, null,
                orderBy, null);
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

    private int deleteHistory(String whereClause, String[] whereArgs) {
        return mDatabase.delete(TABLE_HISTORY, whereClause, whereArgs);
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
