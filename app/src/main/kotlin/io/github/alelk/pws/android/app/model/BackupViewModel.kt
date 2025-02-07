package io.github.alelk.pws.android.app.model

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.android.app.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.backup.model.Backup
import io.github.alelk.pws.backup.model.Song
import io.github.alelk.pws.backup.model.SongNumber
import io.github.alelk.pws.backup.model.Tag
import io.github.alelk.pws.database.common.entity.SongNumberTagEntity
import io.github.alelk.pws.database.common.entity.TagEntity
import io.github.alelk.pws.domain.model.BibleRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    db: PwsDatabase,
    private val datastore: DataStore<Preferences>
) : ViewModel() {

  private val bookStatisticDao = db.bookStatisticDao()
  private val favoriteDao = db.favoriteDao()
  private val categoryDao = db.tagDao()
  private val songDao = db.songDao()
  private val tagDao = db.tagDao()
  private val songNumberTagDao = db.songNumberTagDao()
  private val songNumberDao = db.songNumberDao()

  suspend fun getBackup(): Backup {
    val favorites = favoriteDao.getAll().first().map { f -> SongNumber(f.bookId, f.songNumber) }
    val editedSongs = songDao.getAllEdited().map { s ->
      Song(
        number = SongNumber(s.bookId, s.songNumber),
        id = s.songId,
        name = s.songName,
        locale = s.songLocale,
        version = s.songVersion,
        lyric = s.songText,
        tonalities = s.songTonalities,
        author = s.songAuthor,
        translator = s.songTranslator,
        composer = s.songComposer,
        bibleRef = s.bibleReferences?.let(::BibleRef)
      )
    }
    val customTags = categoryDao.getAllNotPredefined().map { t ->
      val tagSongs = songNumberTagDao.getSongDetailsByTagId(t.id)
      Tag(name = t.name, color = t.color, songs = tagSongs.map { SongNumber(it.bookId, it.songNumber) }.toSet())
    }
    val bookPreferences = bookStatisticDao.getUserPreferredBooks().mapNotNull {
      it.bookStatistic.userPreference?.let { pref ->
        io.github.alelk.pws.backup.model.BookPreference(it.book.externalId, pref)
      }
    }

    suspend fun <K> setting(key: Preferences.Key<K>, valueMapper: (K) -> String = { it.toString() }) =
      datastore.data.map { it[key] }.firstOrNull()?.let { key.name to valueMapper(it) }

    val settings = listOfNotNull(
      setting(AppPreferenceKeys.SONG_TEXT_SIZE),
      setting(AppPreferenceKeys.SONG_TEXT_EXPANDED),
      setting(AppPreferenceKeys.APP_THEME),
    ).toMap()
    return Backup(songs = editedSongs, favorites = favorites, tags = customTags, bookPreferences = bookPreferences, settings = settings)
  }

  suspend fun restoreBackup(backup: Backup) {
    // restore edited songs
    backup.songs?.forEach { song ->
      val songNumber = songNumberDao.getByBookExternalIdAndSongNumber(song.number.bookId, song.number.number)
      if (songNumber != null) {
        val updatedSong =
          songDao.getById(songNumber.first.songId)
            ?.copy(
              name = song.name,
              lyric = song.lyric,
              tonalities = song.tonalities,
              bibleRef = song.bibleRef?.text,
              author = song.author,
              translator = song.translator,
              composer = song.composer,
              edited = true
            )
        if (updatedSong != null) {
          songDao.update(updatedSong)
        } else {
          Timber.e("song not found for song number: bookId=${song.number.bookId}, number=${song.number.number}")
        }
      } else {
        Timber.e("song number not found for song: bookId=${song.number.bookId}, number=${song.number.number}")
      }
    }

    // restore favorites
    backup.favorites?.forEach { favorite ->
      val sn = songNumberDao.getByBookExternalIdAndSongNumber(favorite.bookId, favorite.number)
      if (sn != null) {
        favoriteDao.addToFavorites(checkNotNull(sn.first.id))
      } else {
        Timber.e("song number not found for favorite: bookId=${favorite.bookId}, number=${favorite.number}")
      }
    }

    // restore tags
    backup.tags?.forEach { tag ->
      val existingTag = categoryDao.getAllByName(tag.name).firstOrNull()
      val tagEntity = if (existingTag == null) {
        // create new tag if it doesn't exist
        val newTag = TagEntity(
          id = tagDao.getNextCustomTagId(),
          name = tag.name,
          color = tag.color,
          predefined = false,
          priority = 0
        )
        categoryDao.insert(newTag)
        newTag
      } else {
        // update existing tag
        val updatedTag = existingTag.copy(color = tag.color)
        categoryDao.update(updatedTag)
        updatedTag
      }
      // add tag songs
      tag.songs.forEach { songNumber ->
        val sn = songNumberDao.getByBookExternalIdAndSongNumber(songNumber.bookId, songNumber.number)
        if (sn != null) {
          if (songNumberTagDao.getById(checkNotNull(sn.first.id), tagEntity.id) == null) {
            val songNumberTagEntity = SongNumberTagEntity(
              songNumberId = checkNotNull(sn.first.id),
              tagId = tagEntity.id
            )
            songNumberTagDao.insert(songNumberTagEntity)
          }
        } else {
          Timber.e("Song number not found for tag assignment: bookShortName=${songNumber.bookId}, number=${songNumber.number}")
        }
      }
    }

    // restore book preferences
    backup.bookPreferences?.forEach { bookPreference ->
      bookStatisticDao.getByBookExternalId(bookPreference.bookId)?.let {
        val updatedBookStatistic = it.bookStatistic.copy(userPreference = bookPreference.preference)
        bookStatisticDao.update(updatedBookStatistic)
      }
    }

    // restore app settings
    val settings = backup.settings

    suspend fun <K> applySetting(key: Preferences.Key<K>, valueMapper: (String) -> K) =
      datastore.edit { preferences ->
        val value =
          settings?.get(key.name)?.let {
            runCatching { valueMapper(it) }
              .onFailure { e -> Timber.e(e, "error restoring setting ${key.name} from backup. Invalid value: '$it'") }
              .getOrNull()
          }
        if (value != null) preferences[key] = value
      }

    if (settings != null) {
      applySetting(AppPreferenceKeys.SONG_TEXT_SIZE) { it.toFloat() }
      applySetting(AppPreferenceKeys.SONG_TEXT_EXPANDED) { it.toBoolean() }
      applySetting(AppPreferenceKeys.APP_THEME) { checkNotNull(AppTheme.byIdentifier(it)?.identifier) { "unknown app theme: '$it'" } }
    }
  }
}