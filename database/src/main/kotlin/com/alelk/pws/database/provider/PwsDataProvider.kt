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
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.alelk.pws.database.BuildConfig
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.database.dao.SongDao
import timber.log.Timber

/** Pws data provider
 *
 * Created by Alex Elkin on 21.05.2015.
 */
class PwsDataProvider : ContentProvider() {

  private lateinit var songDao: SongDao

  override fun onCreate(): Boolean {
    val db = DatabaseProvider.getDatabase(context!!)
    songDao = db.songDao()
    return true
  }

  override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
    val cursor =
      when (URI_MATCHER.match(uri)) {

        URI_MATCH_SUGGESTIONS_BY_NUMBER -> {
          val songNumber = uri.lastPathSegment?.toIntOrNull() ?: return null
          val limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT)?.toIntOrNull()
          songDao
            .getSuggestionsBySongNumber(songNumber, limit ?: 50)
            .also { Timber.d("get songs suggestions by song number $songNumber with limit $limit: count = ${it.count}") }
        }

        URI_MATCH_SUGGESTIONS_BY_NAME -> {
          val searchText = uri.lastPathSegment?.trim()?.replace(Regex("\\s+"), "*")?.let { "$it*" } ?: return null
          val limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT)?.toIntOrNull()
          val result = songDao
            .getSuggestionsBySongName(searchText, limit ?: 50)
            .takeIf { it.count > 0 }
            ?: songDao.getSuggestionsBySongLyric(searchText, limit ?: 50)
          Timber.d("get songs suggestions by text $searchText with limit $limit: count = ${result.count}")
          result
        }

        else -> {
          Timber.w("unknown query: $uri")
          null
        }
      }
    return cursor?.apply { setNotificationUri(context!!.contentResolver, uri) }
  }

  override fun getType(uri: Uri): String? = null
  override fun insert(uri: Uri, values: ContentValues?): Uri? = null
  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = -1
  override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = -1


  companion object {
    const val URI_MATCH_SUGGESTIONS_BY_NUMBER = 1
    const val URI_MATCH_SUGGESTIONS_BY_NAME = 2

    private val URI_MATCHER =
      UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(BuildConfig.DB_AUTHORITY, "songs/${SearchManager.SUGGEST_URI_PATH_QUERY}/#", URI_MATCH_SUGGESTIONS_BY_NUMBER)
        addURI(BuildConfig.DB_AUTHORITY, "songs/${SearchManager.SUGGEST_URI_PATH_QUERY}/*", URI_MATCH_SUGGESTIONS_BY_NAME)
      }
  }
}