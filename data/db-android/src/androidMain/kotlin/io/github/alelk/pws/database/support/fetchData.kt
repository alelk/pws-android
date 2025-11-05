package io.github.alelk.pws.database.support

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import timber.log.Timber

internal suspend fun <T> SQLiteDatabase.fetchData(
  collectionName: String,
  table: String,
  columns: Array<String>,
  dbVersion: IntRange,
  selection: String? = null,
  selectionArgs: Array<String>? = null,
  extractor: (cursor: Cursor) -> T
): Result<List<T>> =
  runCatching {
    if (this.version !in dbVersion) {
      Timber.d("skip fetching '$collectionName' from pws database version=$version, path=$path (supported database version: $dbVersion)")
      emptyList()
    } else {
      Timber.i("get '$collectionName' from pws database version=$version, path=$path ...")
      val entities =
        query(false, table, columns, selection, selectionArgs, null, null, null, null)
          .use { cursor ->
            if (!cursor.moveToFirst()) emptyList()
            else flow {
              do {
                val data = runCatching { extractor(cursor) }
                emit(data)
              } while (cursor.moveToNext())
            }.toList()
              .distinct()
              .also { Timber.i("${it.filter { v -> v.isSuccess }.size} '$collectionName' fetched from pws database version=$version, path=$path") }
          }
      val errors = entities.mapNotNull { it.exceptionOrNull() }.distinctBy { it.message }
      if (errors.isNotEmpty()) {
        Timber.e(
          errors.first(),
          "error fetching '$collectionName' from pws database version=$version, path=$path: " +
            errors.take(3).joinToString(",") { it.message.toString() }
        )
      }
      entities.mapNotNull { it.getOrNull() }
    }
  }.onFailure { exc -> Timber.e(exc, "error fetching '$collectionName' from pws database version=$version, path=$path: ${exc.message}") }