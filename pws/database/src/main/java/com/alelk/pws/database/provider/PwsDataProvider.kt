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

package com.alelk.pws.database.provider

import android.app.SearchManager
import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.alelk.pws.database.helper.PwsDatabaseHelper
import com.alelk.pws.database.provider.PwsDataProvider
import com.alelk.pws.database.provider.PwsDataProviderContract.*
import com.alelk.pws.database.provider.PwsDataProviderContract.Companion.AUTHORITY
import com.alelk.pws.database.provider.PwsDataProviderContract.Companion.HISTORY_TIMESTAMP_FORMAT
import com.alelk.pws.database.provider.PwsDataProviderContract.Companion.QUERY_PARAMETER_LIMIT
import com.alelk.pws.database.table.PwsBookStatisticTable
import com.alelk.pws.database.table.PwsFavoritesTable
import com.alelk.pws.database.table.PwsHistoryTable
import com.alelk.pws.database.table.PwsPsalmTable
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pws data provider
 *
 * Created by Alex Elkin on 21.05.2015.
 *
 * TODO 2022.10.20: too legacy and ugly code :(
 */
class PwsDataProvider : ContentProvider(), PwsDataProviderContract {
  private var mContext: Context? = null
  private var mDatabase: SQLiteDatabase? = null
  private var mDatabaseHelper: PwsDatabaseHelper? = null
  var mDateFormat = SimpleDateFormat(HISTORY_TIMESTAMP_FORMAT, Locale.ENGLISH)
  override fun onCreate(): Boolean {
    mContext = context
    if (mContext == null) return false
    mDatabaseHelper = PwsDatabaseHelper(mContext!!)
    return true
  }

  override fun query(
    uri: Uri,
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    sortOrder: String?
  ): Cursor? {
    val METHOD_NAME = "query"
    mDatabase = mDatabaseHelper!!.readableDatabase
    var cursor: Cursor? = null
    val psalmNumberId: Long
    when (URI_MATCHER.match(uri)) {
      Psalms.URI_MATCH -> cursor = mDatabase!!.query(
        PwsPsalmTable.TABLE_PSALMS,
        projection,
        selection,
        selectionArgs,
        null,
        null,
        sortOrder
      )
      Psalms.URI_MATCH_ID -> {}
      Psalms.PsalmNumbers.URI_MATCH -> {}
      Favorites.URI_MATCH -> cursor =
        queryFavorites(projection, selection, selectionArgs, null, null)
      Favorites.URI_MATCH_ID -> cursor = queryFavorite(uri.lastPathSegment.toLong())
      History.URI_MATCH -> cursor = queryHistory(
        projection,
        selection,
        selectionArgs,
        null,
        uri.getQueryParameter(QUERY_PARAMETER_LIMIT)
      )
      History.Last.URI_MATCH -> cursor =
        queryHistory(projection, null, null, History.Last.SORT_ORDER, History.Last.LIMIT)
      Psalms.Suggestions.URI_MATCH_NUMBER -> cursor = querySuggestionsPsalmNumber(
        uri.lastPathSegment,
        uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT)
      )
      Psalms.Suggestions.URI_MATCH_NAME ->
        if (uri.lastPathSegment != null) {
          val searchText = uri.lastPathSegment
          cursor = searchText?.let {
            querySuggestionsPsalmName(
              it,
              uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT)
            )
          }
          if (cursor == null || cursor.count < 1) cursor = querySuggestionsPsalmText(
            searchText,
            uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT)
          )
        }
      Psalms.Search.URI_MATCH -> {
        if (selectionArgs?.isNotEmpty() == true) {
          try {
            val num = selectionArgs[0].toInt()
            cursor = querySearchPsalmNumber(num, "50")
          } catch (ex: NumberFormatException) {
            var text =
              if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) selectionArgs[0].lowercase(
                Locale.getDefault()
              ) else selectionArgs[0]
            text = text.trim { it <= ' ' }.replace("\\s++".toRegex(), "* NEAR/6 ") + "*"
            cursor = querySearchPsalmText(text, "50")
          }
        }
      }
      PsalmNumbers.Psalm.URI_MATCH -> {
        psalmNumberId = uri.pathSegments[1].toLong()
        cursor = queryPsalmNumberPsalm(psalmNumberId, projection, selection, selectionArgs)
      }
      PsalmNumbers.Book.BookPsalmNumbers.URI_MATCH -> {
        psalmNumberId = uri.pathSegments[1].toLong()
        cursor = queryPsalmNumberBookPsalmNumbers(psalmNumberId, projection)
      }
      PsalmNumbers.Book.BookPsalmNumbers.Info.URI_MATCH -> {
        psalmNumberId = uri.pathSegments[1].toLong()
        cursor = queryPsalmNumberBookPsalmNumberInfo(psalmNumberId, projection)
      }
      PsalmNumbers.ReferencePsalms.URI_MATCH -> {
        psalmNumberId = uri.pathSegments[1].toLong()
        cursor = queryPsalmNumberReferredPsalms(psalmNumberId)
      }
      BookStatistic.URI_MATCH -> cursor = queryBookStatistic(null, null, null, null)
      else -> Log.w(LOG_TAG, "$METHOD_NAME: Incorrect uri: '$uri'")
    }
    if (cursor == null) {
      Log.d(LOG_TAG, "$METHOD_NAME: No results for uri: '$uri'")
      return null
    }
    Log.v(
      LOG_TAG,
      METHOD_NAME + ": Query for uri='" + uri.toString() + "'. Results: " + cursor.count
    )
    cursor.setNotificationUri(mContext!!.contentResolver, uri)
    return cursor
  }

  override fun getType(uri: Uri): String? =
    when (URI_MATCHER.match(uri)) {
      Psalms.URI_MATCH ->
        "vnd.android.cursor.dir/" + AUTHORITY + "." + Psalms.PATH
      Psalms.URI_MATCH_ID ->
        "vnd.android.cursor.item/" + AUTHORITY + "." + Psalms.PATH
      else -> null
    }

  override fun insert(uri: Uri, values: ContentValues?): Uri? {
    val METHOD_NAME = "insert"
    Log.v(LOG_TAG, "$METHOD_NAME: uri='$uri'")
    mDatabase = mDatabaseHelper!!.writableDatabase
    var itemUri: Uri? = null
    val id: Long
    when (URI_MATCHER.match(uri)) {
      Psalms.URI_MATCH -> {
        id = mDatabase!!.insert(PwsPsalmTable.TABLE_PSALMS, null, values)
        if (id == -1L) {
          // TODO: 24.02.2016 throw exception
          Log.w(
            LOG_TAG,
            METHOD_NAME + ": Error inserting into '" + PwsPsalmTable.TABLE_PSALMS + "' table. Uri='" + uri + "'"
          )
          return null
        }
        itemUri = ContentUris.withAppendedId(uri, id)
      }
      Psalms.URI_MATCH_ID -> {}
      Favorites.URI_MATCH -> {
        id = insertFavorite(values!!)
        if (id == -1L) {
          // TODO: 24.02.2016 throw exception
          Log.w(
            LOG_TAG,
            METHOD_NAME + ": Error inserting into '" + PwsFavoritesTable.TABLE_FAVORITES + "' table. Uri='" + uri + "'"
          )
          return null
        }
        itemUri = ContentUris.withAppendedId(uri, id)
        mContext!!.contentResolver.notifyChange(uri, null)
      }
      History.URI_MATCH -> {
        id = insertHistory(values!!)
        if (id == -1L) {
          // TODO: 24.02.2016 throw exception
          Log.w(
            LOG_TAG,
            METHOD_NAME + ": Error inserting into '" + PwsHistoryTable.TABLE_HISTORY + "' table. Uri='" + uri + "'"
          )
          return null
        }
        itemUri = ContentUris.withAppendedId(uri, id)
        mContext!!.contentResolver.notifyChange(uri, null)
      }
      else ->                 // TODO: 24.02.2016 throw exception incorrect uri
        Log.w(LOG_TAG, "$METHOD_NAME: Incorrect uri. Uri='$uri'")
    }
    return itemUri!!
  }

  override fun delete(uri: Uri, selection: String, selectionArgs: Array<String>): Int {
    val METHOD_NAME = "delete"
    Log.v(LOG_TAG, "$METHOD_NAME: uri='$uri'")
    mDatabase = mDatabaseHelper!!.writableDatabase
    var n = 0
    when (URI_MATCHER.match(uri)) {
      Favorites.URI_MATCH -> n = deleteFavorites(selection, selectionArgs)
      History.URI_MATCH -> n = deleteHistory(selection, selectionArgs)
      else -> {}
    }
    mContext!!.contentResolver.notifyChange(uri, null)
    return n
  }

  override fun update(
    uri: Uri,
    values: ContentValues,
    selection: String,
    selectionArgs: Array<String>
  ): Int {
    val METHOD_NAME = "update"
    Log.v(LOG_TAG, "$METHOD_NAME: uri='$uri'")
    mDatabase = mDatabaseHelper!!.writableDatabase
    var m = 0
    when (URI_MATCHER.match(uri)) {
      BookStatistic.URI_MATCH_TEXT -> m = updateBookStatistic(values, uri.lastPathSegment)
    }
    return m
  }

  private fun queryFavorites(
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    orderBy: String?,
    limit: String?
  ): Cursor? {
    var projection = projection
    var orderBy = orderBy
    val METHOD_NAME = "queryFavorites"
    if (projection == null) projection = Favorites.PROJECTION
    if (orderBy == null) orderBy = Favorites.SORT_ORDER
    val cursor = mDatabase!!.query(
      Favorites.TABLES,
      projection, selection, selectionArgs,
      Favorites.GROUP_BY, null,
      orderBy, limit
    )
    Log.v(
      LOG_TAG, METHOD_NAME + ": projection=" + Arrays.toString(projection) +
        " selection='" + selection + "' selectionArgs=" + Arrays.toString(selectionArgs) +
        " orderBy='" + orderBy + "' limit=" + limit +
        " results: " + (cursor?.count ?: "cursor=null")
    )
    return cursor
  }

  private fun queryHistory(
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    orderBy: String?,
    limit: String?
  ): Cursor {
    var projection = projection
    var orderBy = orderBy
    if (projection == null) projection = History.PROJECTION
    if (orderBy == null) orderBy = History.SORT_ORDER
    return mDatabase!!.query(
      History.TABLES,
      projection, selection, selectionArgs,
      History.GROUP_BY, null,
      orderBy, limit
    )
  }

  private fun queryFavorite(id: Long): Cursor? {
    val selectionArgs = arrayOf(id.toString())
    return queryFavorites(null, Favorites.SELECTION_ID_MATCH, selectionArgs, null, null)
  }

  private fun queryLastFavorite(projection: Array<String>?): Cursor? {
    val METHOD_NAME = "queryLastFavorite"
    val cursor = queryFavorites(projection, null, null, null, "1")
    Log.v(
      LOG_TAG,
      METHOD_NAME + ": projection=" + Arrays.toString(projection) + " results: " + (cursor?.count
        ?: "cursor=null")
    )
    return cursor
  }

  private fun querySuggestionsPsalmNumber(
    psalmNumber: String,
    limit: String?
  ): Cursor {
    return mDatabase!!.query(
      Psalms.Suggestions.SGNUM_TABLES,
      Psalms.Suggestions.SGNUM_PROJECTION,
      Psalms.Suggestions.getSgNumberSelection(psalmNumber),
      null,
      Psalms.Suggestions.COLUMN_PSALMID,
      null,
      Psalms.Suggestions.SGNUM_SORT_ORDER,
      limit
    )
  }

  private fun querySuggestionsPsalmName(searchName: String, limit: String?): Cursor {
    return mDatabase!!.query(
      Psalms.Suggestions.SG_NAME_TABLES,
      Psalms.Suggestions.SG_NAME_PROJECTION,
      Psalms.Suggestions.getSgNameSelection(searchName.lowercase(Locale.getDefault())),
      null,
      Psalms.Suggestions.SG_NAME_GROUPBY,
      null,
      null,
      limit
    )
  }

  private fun querySuggestionsPsalmText(searchName: String, limit: String?): Cursor {
    return mDatabase!!.query(
      Psalms.Suggestions.SG_TXT_TABLES,
      Psalms.Suggestions.SG_TXT_PROJECTION,
      Psalms.Suggestions.getSgTextSelection(searchName.lowercase(Locale.getDefault())),
      null,
      Psalms.Suggestions.SG_TXT_GROUPBY,
      null,
      null,
      limit
    )
  }

  private fun querySearchPsalmNumber(
    psalmNumber: Int,
    limit: String?
  ): Cursor {
    return mDatabase!!.query(
      Psalms.Search.S_NUM_TABLES,
      Psalms.Search.S_NUM_PROJECTION,
      Psalms.Search.getSNumSelection(psalmNumber),
      null, null, null,
      Psalms.Search.S_NUM_ORDER_BY,
      limit
    )
  }

  private fun querySearchPsalmText(
    searchText: String,
    limit: String?
  ): Cursor? {
    val METHOD_NAME = "querySearchPsalmText"
    val cursor = mDatabase!!.query(
      Psalms.Search.S_TXT_TABLES,
      Psalms.Search.S_TXT_PROJECTION, Psalms.Search.getSTxtSelection(searchText), null,
      null, null,
      Psalms.Search.S_TXT_ORDER_BY, limit
    )
    Log.d(
      LOG_TAG, METHOD_NAME + ": searchQuery='" + searchText + "' results:" + (cursor?.count
        ?: 0)
    )
    return cursor
  }

  private fun queryPsalmNumberPsalm(
    psalmNumberId: Long,
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?
  ): Cursor {
    var projection = projection
    var selection = selection
    var selectionArgs = selectionArgs
    if (projection == null) projection = PsalmNumbers.Psalm.PROJECTION
    if (selection == null) {
      selection = PsalmNumbers.Psalm.DEFAULT_SELECTION
      selectionArgs = arrayOf(java.lang.Long.toString(psalmNumberId))
    }
    return mDatabase!!.query(
      PsalmNumbers.Psalm.TABLES,
      projection,
      selection, selectionArgs, null, null,
      null
    )
  }

  private fun queryPsalmNumberBookPsalmNumbers(
    psalmNumberId: Long,
    projection: Array<String>?
  ): Cursor? {
    return queryPsalmNumberBookPsalmNumber(psalmNumberId, projection, null, null)
  }

  private fun queryPsalmNumberBookPsalmNumber(
    psalmNumberId: Long,
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String?>?
  ): Cursor? {
    var projection = projection
    var selectionArgs = selectionArgs
    val METHOD_NAME = "queryPsalmNumberBookPsalmNumber"
    if (projection == null) projection = PsalmNumbers.Book.BookPsalmNumbers.PROJECTION
    if (selection == null) selectionArgs = null
    val rawQuery = SQLiteQueryBuilder.buildQueryString(
      false,
      PsalmNumbers.Book.BookPsalmNumbers.buildRawTables(psalmNumberId),
      projection, null, null, null,
      PsalmNumbers.Book.BookPsalmNumbers.ORDER_BY, null
    )
    val cursor = mDatabase!!.rawQuery(rawQuery, selectionArgs)
    Log.v(
      LOG_TAG, METHOD_NAME + ": rawQuery='" + rawQuery + "' selectionArgs=" +
        Arrays.toString(selectionArgs) + " results:" + (cursor?.count ?: 0)
    )
    return cursor
  }

  private fun queryPsalmNumberBookPsalmNumberInfo(
    psalmNumberId: Long,
    projection: Array<String>?
  ): Cursor? {
    var projection = projection
    val METHOD_NAME = "queryPsalmNumberBookPsalmNumberInfo"
    if (projection == null) projection =
      PsalmNumbers.Book.BookPsalmNumbers.Info.PROJECTION_PSALMNUMBER_ID
    val rawQuery = SQLiteQueryBuilder.buildQueryString(
      false,
      PsalmNumbers.Book.BookPsalmNumbers.Info.buildRawTables(psalmNumberId),
      projection, null, null, null,
      null, null
    )
    val cursor = mDatabase!!.rawQuery(rawQuery, null)
    Log.v(LOG_TAG, METHOD_NAME + ": rawQuery='" + rawQuery + " results:" + (cursor?.count ?: 0))
    return cursor
  }

  private fun queryPsalmNumberReferredPsalms(currentPsalmNumberId: Long): Cursor? {
    val METHOD_NAME = "queryPsalmNumberReferredPsalms"
    val rawQuery = SQLiteQueryBuilder.buildQueryString(
      false,
      PsalmNumbers.ReferencePsalms.buildRawTables(currentPsalmNumberId),
      PsalmNumbers.ReferencePsalms.PROJECTION, null,
      PsalmNumbers.ReferencePsalms.COLUMN_PSALM_ID, null,
      null, null
    )
    val cursor = mDatabase!!.rawQuery(rawQuery, null)
    Log.v(LOG_TAG, METHOD_NAME + ": rawQuery='" + rawQuery + " results:" + (cursor?.count ?: 0))
    return cursor
  }

  private fun queryBookStatistic(
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    orderBy: String?
  ): Cursor {
    var projection = projection
    var orderBy = orderBy
    if (projection == null) projection = BookStatistic.PROJECTION
    if (orderBy == null) orderBy = BookStatistic.SORT_ORDER
    return mDatabase!!.query(
      BookStatistic.TABLES,
      projection, selection, selectionArgs,
      null, null,
      orderBy, null
    )
  }

  /*
  private Cursor queryPsalmNumberMorePreferred(long psalmId) {
      final String orderBy = "pn." + PwsPsalmNumbersTable.COLUMN_BOOKID;
      // TODO: 01.03.2016 add preferred book selection logic
      return queryPsalmNumbers(psalmId, null, orderBy, "1");
  } */
  private fun insertFavorite(values: ContentValues): Long {
    val METHOD_NAME = "insertFavorite"
    val lastFavorite = queryLastFavorite(null)
    var favoritePosition: Long = 2
    if (lastFavorite != null && lastFavorite.moveToFirst()) {
      favoritePosition =
        1 + lastFavorite.getLong(lastFavorite.getColumnIndex(Favorites.COLUMN_FAVORITEPOSITION))
    }
    if (values.containsKey(Favorites.COLUMN_FAVORITEPOSITION)) {
      val valuePosition = values.getAsLong(Favorites.COLUMN_FAVORITEPOSITION)
      if (valuePosition < favoritePosition) {
        // TODO: 29.02.2016 shift favorites list
        Log.w(
          LOG_TAG, METHOD_NAME + ": Try to insert favorite with position='" +
            valuePosition + "'. Error inserting: unable to shift favorites list. "
        )
      }
    }
    values.put(PwsFavoritesTable.COLUMN_POSITION, favoritePosition)
    var id: Long = 0
    id = try {
      mDatabase!!.insert(PwsFavoritesTable.TABLE_FAVORITES, null, values)
    } finally {
      Log.v(
        LOG_TAG, METHOD_NAME + ": resultId=" + id + " " +
          "values=[keySet=${values.keySet().joinToString(",")} " +
          "valueSet=${values.valueSet().joinToString(",")}]"
      )
    }
    return id
  }

  private fun insertHistory(values: ContentValues): Long {
    val METHOD_NAME = "insertHistory"
    if (!values.containsKey(History.COLUMN_HISTORYTIMESTAMP)) {
      val timestamp = mDateFormat.format(Date())
      values.put(PwsHistoryTable.COLUMN_ACCESSTIMESTAMP, timestamp)
    }
    var id: Long = 0
    id = try {
      mDatabase!!.insert(PwsHistoryTable.TABLE_HISTORY, null, values)
    } finally {
      Log.v(
        LOG_TAG,
        "$METHOD_NAME: resultId=$id values=[keySet=${values.keySet().joinToString(",")}" +
          " valueSet=${values.valueSet().joinToString(",")}]"
      )
    }
    return id
  }

  private fun deleteFavorites(whereClause: String, whereArgs: Array<String>): Int {
    return mDatabase!!.delete(PwsFavoritesTable.TABLE_FAVORITES, whereClause, whereArgs)
  }

  private fun deleteHistory(whereClause: String, whereArgs: Array<String>): Int {
    return mDatabase!!.delete(PwsHistoryTable.TABLE_HISTORY, whereClause, whereArgs)
  }

  private fun updateBookStatistic(values: ContentValues, bookEdition: String): Int {
    val rawSelection = SQLiteQueryBuilder.buildQueryString(
      false,
      BookStatistic.RAW_TABLES,
      BookStatistic.RAW_PROJECTION,
      BookStatistic.getRawSelection(bookEdition), null, null, null, null
    )
    return mDatabase!!.update(
      PwsBookStatisticTable.TABLE_BOOKSTATISTIC,
      values, "_id=($rawSelection)",
      null
    )
  }

  companion object {
    private val LOG_TAG = PwsDataProvider::class.java.simpleName
    private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH)

    init {
      URI_MATCHER.addURI(AUTHORITY, Psalms.PATH, Psalms.URI_MATCH)
      URI_MATCHER.addURI(AUTHORITY, Psalms.PATH_ID, Psalms.URI_MATCH_ID)
      URI_MATCHER.addURI(AUTHORITY, Psalms.PsalmNumbers.PATH, Psalms.PsalmNumbers.URI_MATCH)
      URI_MATCHER.addURI(
        AUTHORITY,
        Psalms.PsalmNumbers.PATH_ID,
        Psalms.PsalmNumbers.URI_MATCH_ID
      )
      URI_MATCHER.addURI(
        AUTHORITY,
        Psalms.Suggestions.PATH_NUMBER,
        Psalms.Suggestions.URI_MATCH_NUMBER
      )
      URI_MATCHER.addURI(
        AUTHORITY,
        Psalms.Suggestions.PATH_NAME,
        Psalms.Suggestions.URI_MATCH_NAME
      )
      URI_MATCHER.addURI(AUTHORITY, Psalms.Search.PATH, Psalms.Search.URI_MATCH)
      URI_MATCHER.addURI(AUTHORITY, Favorites.PATH, Favorites.URI_MATCH)
      URI_MATCHER.addURI(AUTHORITY, Favorites.PATH_ID, Favorites.URI_MATCH_ID)
      URI_MATCHER.addURI(AUTHORITY, History.PATH, History.URI_MATCH)
      URI_MATCHER.addURI(AUTHORITY, History.PATH_ID, History.URI_MATCH_ID)
      URI_MATCHER.addURI(AUTHORITY, History.Last.PATH, History.Last.URI_MATCH)
      URI_MATCHER.addURI(AUTHORITY, PsalmNumbers.PATH, PsalmNumbers.URI_MATCH)
      URI_MATCHER.addURI(AUTHORITY, PsalmNumbers.PATH_ID, PsalmNumbers.URI_MATCH_ID)
      URI_MATCHER.addURI(AUTHORITY, PsalmNumbers.Psalm.PATH, PsalmNumbers.Psalm.URI_MATCH)
      URI_MATCHER.addURI(
        AUTHORITY,
        PsalmNumbers.Book.BookPsalmNumbers.PATH,
        PsalmNumbers.Book.BookPsalmNumbers.URI_MATCH
      )
      URI_MATCHER.addURI(
        AUTHORITY,
        PsalmNumbers.Book.BookPsalmNumbers.Info.PATH,
        PsalmNumbers.Book.BookPsalmNumbers.Info.URI_MATCH
      )
      URI_MATCHER.addURI(
        AUTHORITY,
        PsalmNumbers.ReferencePsalms.PATH,
        PsalmNumbers.ReferencePsalms.URI_MATCH
      )
      URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH, BookStatistic.URI_MATCH)
      URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH_ID, BookStatistic.URI_MATCH_ID)
      URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH_TEXT, BookStatistic.URI_MATCH_TEXT)
    }
  }
}