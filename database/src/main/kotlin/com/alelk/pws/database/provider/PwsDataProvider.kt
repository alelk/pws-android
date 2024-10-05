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
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.alelk.pws.database.helper.PwsDatabaseHelper
import com.alelk.pws.database.provider.PwsDataProviderContract.BookStatistic
import com.alelk.pws.database.provider.PwsDataProviderContract.Books
import com.alelk.pws.database.provider.PwsDataProviderContract.Companion.AUTHORITY
import com.alelk.pws.database.provider.PwsDataProviderContract.Psalms
import com.alelk.pws.database.table.PwsBookStatisticTable
import java.util.Locale

/**
 * Pws data provider
 *
 * Created by Alex Elkin on 21.05.2015.
 *
 * TODO 2022.10.20: too legacy and ugly code :(
 */
// todo: migrate to room
@Deprecated("use room")
class PwsDataProvider : ContentProvider(), PwsDataProviderContract {
  private var mContext: Context? = null
  private var mDatabase: SQLiteDatabase? = null
  private var mDatabaseHelper: PwsDatabaseHelper? = null
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
    when (URI_MATCHER.match(uri)) {


      Psalms.Suggestions.URI_MATCH_NUMBER -> cursor = querySuggestionsPsalmNumber(
        uri.lastPathSegment!!,
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
          if ((cursor == null || cursor.count < 1) && searchText != null) cursor =
            querySuggestionsPsalmText(
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

      BookStatistic.URI_MATCH -> cursor = queryBookStatistic(null, null, null, null)

      Books.URI_MATCH -> cursor = mDatabase!!.query(
        Books.TABLE, Books.PROJECTION, Books.ACTIVE + " and " + Books.FIRST_SONG,
        selectionArgs,
        null,
        null,
        Books.SORTED
      )

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
    TODO("Not yet implemented")
  }

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
    TODO("Not yet implemented")
  }

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<String>?
  ): Int {
    val METHOD_NAME = "update"
    Log.v(LOG_TAG, "$METHOD_NAME: uri='$uri'")
    mDatabase = mDatabaseHelper!!.writableDatabase
    var m = 0
    if (values != null) {
      when (URI_MATCHER.match(uri)) {
        BookStatistic.URI_MATCH_TEXT -> m = updateBookStatistic(values, uri.lastPathSegment!!)
      }
    }
    return m
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
      URI_MATCHER.addURI(AUTHORITY, Psalms.Suggestions.PATH_NUMBER, Psalms.Suggestions.URI_MATCH_NUMBER)
      URI_MATCHER.addURI(AUTHORITY, Psalms.Suggestions.PATH_NAME, Psalms.Suggestions.URI_MATCH_NAME)
      URI_MATCHER.addURI(AUTHORITY, Psalms.Search.PATH, Psalms.Search.URI_MATCH)
      URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH, BookStatistic.URI_MATCH)
      URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH_ID, BookStatistic.URI_MATCH_ID)
      URI_MATCHER.addURI(AUTHORITY, BookStatistic.PATH_TEXT, BookStatistic.URI_MATCH_TEXT)
    }
  }
}