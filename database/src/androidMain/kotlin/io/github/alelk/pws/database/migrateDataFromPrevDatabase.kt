package io.github.alelk.pws.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider.DATABASE_NAME
import io.github.alelk.pws.database.entity.HistoryEntity
import io.github.alelk.pws.database.entity.SongTagEntity
import io.github.alelk.pws.database.entity.TagEntity
import io.github.alelk.pws.database.support.PwsDb1xDataProvider
import io.github.alelk.pws.domain.model.SongNumber
import timber.log.Timber

internal val DATABASE_PREV_NAMES = arrayOf("pws.1.8.0.db", "pws.1.2.0.db", "pws.1.1.0.db", "pws.0.9.1.db")

internal suspend fun migrateDataFromPrevDatabase(context: Context, currentDatabase: PwsDatabase): Boolean =
  DATABASE_PREV_NAMES.map { dbName ->
    runCatching {
      val dbFile = context.getDatabasePath(dbName)
      val prevDb = dbFile?.let(::openReadOnlyDatabase)
      if (prevDb != null) {
        prevDb.migrateDataTo(currentDatabase)
          .onSuccess {
            // delete previous database file if all user data has been upserted
            Timber.i("imported all user data from previous database $dbFile, delete file...")
            dbFile.delete()
          }.getOrThrow()
      } else Timber.d("no previous database $dbFile found")
    }.onFailure { e ->
      Timber.e(e, "error migrating data from previous database $dbName: ${e.message}")
    }
  }.all { it.isSuccess }

internal suspend fun SQLiteDatabase.migrateDataTo(currentDatabase: PwsDatabase): Result<Unit> =
  kotlin.runCatching {
    Timber.i("found previous database $path ($version), migrate user data to $DATABASE_NAME...")
    val dataProvider = PwsDb1xDataProvider(this)
    val favorites = dataProvider.getFavorites()
    val history = dataProvider.getHistory()
    val editedSongs = dataProvider.getEditedSongs()
    val tags = dataProvider.getTags()

    suspend fun getSongNumberEntities(songNumbers: List<SongNumber>) =
      songNumbers
        .groupBy { it.bookId }
        .mapValues { (_, values) -> values.map { it.number }.distinct() }
        .flatMap { (bookId, sn) -> currentDatabase.songNumberDao().getByBookIdAndSongNumbers(bookId, sn) }

    // upsert favorites
    favorites.onSuccess { songNumbers ->
      val songNumberIds = getSongNumberEntities(songNumbers).map { it.id }
      val countUpserted = songNumberIds.map { id ->
        runCatching {
          currentDatabase.favoriteDao().addToFavorites(id)
        }.onFailure { e -> Timber.e("error upserting song $id to favorite: ${e.message}") }
      }.count { it.isSuccess }
      Timber.i("$countUpserted of ${songNumbers.size} favorites upserted to database $DATABASE_NAME")
    }

    // upsert history only if there is the first app starting after app upgrade
    if (currentDatabase.historyDao().count() == 0) {
      history.onSuccess { historyItems ->
        val historyEntities =
          historyItems
            .mapNotNull { item ->
              val songNumberId = currentDatabase.songNumberDao().getByBookIdAndSongNumber(item.songNumber.bookId, item.songNumber.number)?.id
              songNumberId?.let { HistoryEntity(it, accessTimestamp = item.timestamp) }
            }
        val countUpserted = historyEntities.map { entity ->
          runCatching {
            currentDatabase.historyDao().insert(entity)
          }.onFailure { e -> Timber.e("error upserting history item $entity to history: ${e.message}") }
        }.count { it.isSuccess }
        Timber.i("$countUpserted of ${historyItems.size} history records upserted to database $DATABASE_NAME")
      }
    }

    // upsert edited songs
    editedSongs.onSuccess { songs ->
      val songIdBySongNumber = getSongNumberEntities(songs.map { it.number }.distinct()).associate { SongNumber(it.bookId, it.number) to it.songId }
      val songChangeBySongId =
        songs.mapNotNull { change ->
          val songId = songIdBySongNumber[change.number]
          songId?.let { it to change }
        }.toMap()
      val countUpdated = songChangeBySongId.map { (songId, change) ->
        runCatching {
          val song = currentDatabase.songDao().getById(songId)
          if (song != null)
            currentDatabase.songDao()
              .update(
                song.copy(lyric = change.lyric, bibleRef = change.bibleRef, tonalities = change.tonalities, edited = true)
              )
        }.onFailure { e -> Timber.e("error updating song #$songId (${change.number}): ${e.message}") }
      }.count { it.isSuccess }
      Timber.i("$countUpdated songs has been updated")
    }

    // upsert tags
    tags.onSuccess { allTags ->
      val songNumbersByTagId = allTags.associate { t -> t.id to t.songNumbers.flatMap { (bookId, numbers) -> numbers.map { SongNumber(bookId, it) } } }
      val allSongNumbers = songNumbersByTagId.values.flatten().distinct()
      val songIdBySongNumber = getSongNumberEntities(allSongNumbers).associate { SongNumber(it.bookId, it.number) to it.songId }
      val predefinedTags = allTags.filter { it.predefined }
      val customTags = allTags - predefinedTags.toSet()
      val customSongIdsByTagId =
        customTags.mapNotNull { tag ->
          runCatching {
            val existingTag = currentDatabase.tagDao().getAllByName(tag.name).firstOrNull()
            val targetTagId =
              if (existingTag != null) {
                Timber.d("existing custom tag '${tag.name}' (${existingTag.id}) found, update tag color")
                currentDatabase.tagDao().update(existingTag.copy(color = tag.color))
                existingTag.id
              } else {
                val tagId = currentDatabase.tagDao().getNextCustomTagId()
                currentDatabase.tagDao().insert(TagEntity(id = tagId, name = tag.name, color = tag.color, priority = 1000, predefined = false))
                Timber.d("new custom tag '${tag.name}' created: $tagId")
                tagId
              }
            val tagSongIds = songNumbersByTagId[tag.id]?.mapNotNull { songIdBySongNumber[it] }?.toSet() ?: emptySet()
            targetTagId to tagSongIds
          }.onFailure { e -> Timber.e("error upserting  custom tag ${tag.name}: ${e.message}") }
            .getOrNull()
        }.toMap()
      val predefinedSongIdsByTagId =
        predefinedTags.map { tag ->
          val tagSongIds = songNumbersByTagId[tag.id]?.mapNotNull { songIdBySongNumber[it] }?.toSet() ?: emptySet()
          tag.id to tagSongIds
        }
      (customSongIdsByTagId + predefinedSongIdsByTagId).map { (tagId, songIds) ->
        val tagEntities = songIds.map { SongTagEntity(it, tagId) }
        runCatching {
          currentDatabase.songTagDao().insertIfMissing(tagEntities)
        }.onFailure { e -> Timber.e("error inserting ${tagEntities.size} tag entities for tag $tagId: ${e.message}") }
      }
    }
    favorites.getOrThrow()
    history.getOrThrow()
    editedSongs.getOrThrow()
    tags.getOrThrow()
  }