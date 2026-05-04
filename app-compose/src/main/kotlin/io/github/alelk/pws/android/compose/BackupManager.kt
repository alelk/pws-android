package io.github.alelk.pws.android.compose

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.alelk.pws.backup.model.Backup
import io.github.alelk.pws.backup.model.BookPreference
import io.github.alelk.pws.backup.model.Song
import io.github.alelk.pws.backup.model.SongNumber
import io.github.alelk.pws.backup.model.Tag
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.song_tag.SongTagEntity
import io.github.alelk.pws.database.tag.TagEntity
import io.github.alelk.pws.features.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class BackupManager(
  private val db: PwsDatabase,
  private val dataStore: DataStore<Preferences>,
) {

  private val bookStatisticDao = db.bookStatisticDao()
  private val favoriteDao = db.favoriteDao()
  private val songDao = db.songDao()
  private val tagDao = db.tagDao()
  private val songTagDao = db.songTagDao()
  private val songNumberDao = db.songNumberDao()

  suspend fun exportBackup(source: String): Backup {
    val favorites = favoriteDao.getAllFavoritesWithSongNumberFlow().first().map { (f, sn) ->
      SongNumber(f.bookId, sn.number)
    }

    val editedSongs = songDao.getAllEdited().map { s ->
      Song(
        number = SongNumber(s.book.id, s.songNumber.number),
        id = s.song.id,
        name = s.song.name,
        locale = s.song.locale,
        version = s.song.version,
        lyric = s.song.lyric,
        tonalities = s.song.tonalities,
        author = s.song.author,
        translator = s.song.translator,
        composer = s.song.composer,
        bibleRef = s.song.bibleRef,
      )
    }.toList()

    val customTags = tagDao.getAllNotPredefined().map { t ->
      val tagSongs = songNumberDao.getAllByTagId(t.id).map { SongNumber(it.bookId, it.number) }.toSet()
      Tag(name = t.name, color = t.color, songs = tagSongs)
    }

    val bookPreferences = bookStatisticDao.getAllActive().mapNotNull {
      it.bookStatistic.priority?.let { pref -> BookPreference(it.book.id, pref) }
    }

    val settings = listOfNotNull(
      setting(appThemeKey),
    ).toMap()

    return Backup(
      metadata = Backup.Metadata(source = source),
      songs = editedSongs,
      favorites = favorites,
      tags = customTags,
      bookPreferences = bookPreferences,
      settings = settings,
    )
  }

  suspend fun restoreBackup(backup: Backup) {
    backup.songs?.forEach { song ->
      val songNumber = songNumberDao.getByBookIdAndSongNumber(song.number.bookId, song.number.number)
      if (songNumber != null) {
        songDao.getById(songNumber.songId)?.copy(
          name = song.name,
          lyric = song.lyric,
          tonalities = song.tonalities,
          bibleRef = song.bibleRef,
          author = song.author,
          translator = song.translator,
          composer = song.composer,
          edited = true,
        )?.let { updatedSong ->
          songDao.update(updatedSong)
        }
      }
    }

    backup.favorites?.forEach { favorite ->
      songNumberDao.getByBookIdAndSongNumber(favorite.bookId, favorite.number)?.let { sn ->
        favoriteDao.addToFavorites(sn.id)
      }
    }

    backup.tags?.forEach { tag ->
      val existingTag = tagDao.getAllByName(tag.name).firstOrNull()
      val tagEntity = if (existingTag == null) {
        TagEntity(
          id = tagDao.getNextCustomTagId(),
          name = tag.name,
          color = tag.color,
          predefined = false,
          priority = 0,
        ).also { tagDao.insert(it) }
      } else {
        existingTag.copy(color = tag.color).also { tagDao.update(it) }
      }

      tag.songs.mapNotNull { songNumber ->
        songNumberDao.getByBookIdAndSongNumber(songNumber.bookId, songNumber.number)?.songId
      }.distinct().forEach { songId ->
        if (songTagDao.getById(songId, tagEntity.id) == null) {
          songTagDao.insert(SongTagEntity(songId = songId, tagId = tagEntity.id))
        }
      }
    }

    backup.bookPreferences?.forEach { pref ->
      bookStatisticDao.getById(pref.bookId)?.let { stat ->
        bookStatisticDao.upsert(stat.copy(priority = pref.preference))
      }
    }

    backup.settings?.get(appThemeKey.name)?.let { themeValue ->
      if (ThemeMode.byIdentifier(themeValue).identifier == themeValue) {
        dataStore.edit { prefs -> prefs[appThemeKey] = themeValue }
      }
    }
  }

  private suspend fun <K> setting(
    key: Preferences.Key<K>,
    mapper: (K) -> String = { it.toString() },
  ): Pair<String, String>? =
    dataStore.data.map { it[key] }.firstOrNull()?.let { key.name to mapper(it) }

  companion object {
    private val appThemeKey = stringPreferencesKey("app-theme")
  }
}

