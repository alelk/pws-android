package io.github.alelk.pws.android.app.feature.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.android.app.feature.preference.AppPreferenceKeys
import io.github.alelk.pws.android.app.core.theme.AppTheme
import io.github.alelk.pws.backup.model.Backup
import io.github.alelk.pws.backup.model.BookPreference
import io.github.alelk.pws.backup.model.Song
import io.github.alelk.pws.backup.model.SongNumber
import io.github.alelk.pws.backup.model.Tag
import io.github.alelk.pws.database.BuildConfig
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.song_tag.SongTagEntity
import io.github.alelk.pws.database.tag.TagEntity
import io.github.alelk.pws.domain.core.Locale
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
  private val songDao = db.songDao()
  private val tagDao = db.tagDao()
  private val songTagDao = db.songTagDao()
  private val songNumberDao = db.songNumberDao()

  suspend fun getBackup(source: String? = null): Backup {
    val favorites = favoriteDao.getAllFavoritesWithSongNumberFlow().first().map { (f, sn) ->
        SongNumber(
            f.bookId,
            sn.number
        )
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
            bibleRef = s.song.bibleRef
        )
    }.toList()
    val customTags = tagDao.getAllNotPredefined().map { t ->
      val tagSongs = songNumberDao.getAllByTagId(t.id).map { SongNumber(it.bookId, it.number) }.toSet()
        Tag(name = t.name, color = t.color, songs = tagSongs)
    }
    val bookPreferences = bookStatisticDao.getAllActive().mapNotNull {
      it.bookStatistic.priority?.let { pref ->
          BookPreference(it.book.id, pref)
      }
    }

    suspend fun <K> setting(key: Preferences.Key<K>, valueMapper: (K) -> String = { it.toString() }) =
      datastore.data.map { it[key] }.firstOrNull()?.let { key.name to valueMapper(it) }

    val settings = listOfNotNull(
      setting(AppPreferenceKeys.SONG_TEXT_SIZE),
      setting(AppPreferenceKeys.SONG_TEXT_EXPANDED),
      setting(AppPreferenceKeys.APP_THEME),
    ).toMap()

    @Suppress("KotlinConstantConditions")
    val locale = when (BuildConfig.FLAVOR) {
      "ru" -> Locale.Companion.RU
      "uk" -> Locale.Companion.UK
      "en" -> Locale.Companion.EN
      else -> null
    }

    return Backup(
        metadata = Backup.Metadata(defaultLocale = locale, source = source),
        songs = editedSongs,
        favorites = favorites,
        tags = customTags,
        bookPreferences = bookPreferences,
        settings = settings
    )
  }

  suspend fun restoreBackup(backup: Backup) {
    // restore edited songs
    backup.songs?.forEach { song ->
      val songNumber = songNumberDao.getByBookIdAndSongNumber(song.number.bookId, song.number.number)
      if (songNumber != null) {
        val updatedSong =
          songDao.getById(songNumber.songId)
            ?.copy(
              name = song.name,
              lyric = song.lyric,
              tonalities = song.tonalities,
              bibleRef = song.bibleRef,
              author = song.author,
              translator = song.translator,
              composer = song.composer,
              edited = true
            )
        if (updatedSong != null) {
          songDao.update(updatedSong)
        } else {
          Timber.Forest.e("song not found for song number: bookId=${song.number.bookId}, number=${song.number.number}")
        }
      } else {
        Timber.Forest.e("song number not found for song: bookId=${song.number.bookId}, number=${song.number.number}")
      }
    }

    // restore favorites
    backup.favorites?.forEach { favorite ->
      val sn = songNumberDao.getByBookIdAndSongNumber(favorite.bookId, favorite.number)
      if (sn != null) {
        favoriteDao.addToFavorites(sn.id)
      } else {
        Timber.Forest.e("song number not found for favorite: bookId=${favorite.bookId}, number=${favorite.number}")
      }
    }

    // restore tags
    backup.tags?.forEach { tag ->
      val existingTag = tagDao.getAllByName(tag.name).firstOrNull()
      val tagEntity = if (existingTag == null) {
        // create new tag if it doesn't exist
        val newTag = TagEntity(
            id = tagDao.getNextCustomTagId(),
            name = tag.name,
            color = tag.color,
            predefined = false,
            priority = 0
        )
        tagDao.insert(newTag)
        newTag
      } else {
        // update existing tag
        val updatedTag = existingTag.copy(color = tag.color)
        tagDao.update(updatedTag)
        updatedTag
      }
      // add tag songs
      tag.songs.mapNotNull { songNumber ->
        songNumberDao.getByBookIdAndSongNumber(songNumber.bookId, songNumber.number)
          ?.songId
          .also {
            if (it == null) Timber.Forest.e("Song number not found for tag assignment: bookId=${songNumber.bookId}, number=${songNumber.number}")
          }
      }.distinct().forEach { songId ->
        if (songTagDao.getById(songId, tagEntity.id) == null) {
          val songTagEntity = SongTagEntity(songId = songId, tagId = tagEntity.id)
          songTagDao.insert(songTagEntity)
        }
      }
    }

    // restore book preferences
    backup.bookPreferences?.forEach { bookPreference ->
      bookStatisticDao.getById(bookPreference.bookId)?.let {
        val updatedBookStatistic = it.copy(priority = bookPreference.preference)
        bookStatisticDao.upsert(updatedBookStatistic)
      }
    }

    // restore app settings
    val settings = backup.settings

    suspend fun <K> applySetting(key: Preferences.Key<K>, valueMapper: (String) -> K) =
      datastore.edit { preferences ->
        val value =
          settings?.get(key.name)?.let {
            runCatching { valueMapper(it) }
              .onFailure { e -> Timber.Forest.e(e, "error restoring setting ${key.name} from backup. Invalid value: '$it'") }
              .getOrNull()
          }
        if (value != null) preferences[key] = value
      }

    if (settings != null) {
      applySetting(AppPreferenceKeys.SONG_TEXT_SIZE) { it.toFloat() }
      applySetting(AppPreferenceKeys.SONG_TEXT_EXPANDED) { it.toBoolean() }
      applySetting(AppPreferenceKeys.APP_THEME) { checkNotNull(AppTheme.Companion.byIdentifier(it)?.identifier) { "unknown app theme: '$it'" } }
    }
  }
}