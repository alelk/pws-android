package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alelk.pws.database.DatabaseProvider
import com.alelk.pws.pwapp.theme.AppTheme
import io.github.alelk.pws.backup.model.Backup
import io.github.alelk.pws.backup.model.Song
import io.github.alelk.pws.backup.model.SongNumber
import io.github.alelk.pws.backup.model.Tag
import io.github.alelk.pws.database.common.entity.SongNumberTagEntity
import io.github.alelk.pws.database.common.entity.TagEntity
import io.github.alelk.pws.domain.model.BibleRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

class BackupViewModel(application: Application) : AndroidViewModel(application) {
  private val appPreferencesViewModel: AppPreferencesViewModel = AppPreferencesViewModel(getApplication())
  private val tagViewModel: TagsViewModel = TagsViewModel(application)

  private val db = DatabaseProvider.getDatabase(application)
  private val bookStatisticDao = db.bookStatisticDao()
  private val favoriteDao = db.favoriteDao()
  private val categoryDao = db.tagDao()
  private val songDao = db.songDao()
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
    val settings = listOfNotNull(
      appPreferencesViewModel.songTextSize.firstOrNull()?.toString()?.let { AppPreferenceKeys.SONG_TEXT_SIZE.name to it },
      appPreferencesViewModel.songTextExpanded.firstOrNull()?.toString()?.let { AppPreferenceKeys.SONG_TEXT_EXPANDED.name to it },
      appPreferencesViewModel.appTheme.firstOrNull()?.identifier?.let { AppPreferenceKeys.APP_THEME.name to it }
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
          id = tagViewModel.getNextCustomTagId(),
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
    if (settings != null) {
      settings[AppPreferenceKeys.SONG_TEXT_SIZE.name]?.toFloatOrNull()?.let { appPreferencesViewModel.setSongTextSize(it) }
      settings[AppPreferenceKeys.SONG_TEXT_EXPANDED.name]?.toBooleanStrictOrNull()?.let { appPreferencesViewModel.setSongTextExpanded(it) }
      settings[AppPreferenceKeys.APP_THEME.name]?.let { AppTheme.byIdentifier(it) }?.let { appPreferencesViewModel.setAppTheme(it) }
    }
  }
}