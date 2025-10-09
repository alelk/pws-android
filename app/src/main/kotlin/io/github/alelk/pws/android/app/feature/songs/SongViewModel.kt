package io.github.alelk.pws.android.app.feature.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.entity.BookEntity
import io.github.alelk.pws.database.entity.FavoriteEntity
import io.github.alelk.pws.database.entity.HistoryEntity
import io.github.alelk.pws.database.entity.SongEntity
import io.github.alelk.pws.database.entity.SongNumberEntity
import io.github.alelk.pws.database.entity.SongReferenceDetailsEntity
import io.github.alelk.pws.database.entity.SongTagEntity
import io.github.alelk.pws.database.entity.TagEntity
import io.github.alelk.pws.domain.model.SongNumberId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

data class SongInfo(
  val song: SongEntity,
  val songNumber: SongNumberEntity,
  val book: BookEntity,
  val tags: List<TagEntity>,
  val favorite: FavoriteEntity?,
  val references: List<SongReferenceDetailsEntity>,
  val allBookNumbers: List<SongNumberEntity>,
) {
  val isFavorite: Boolean get() = favorite != null
  val songNumberId: SongNumberId get() = checkNotNull(songNumber.id) { "song number id cannot be null" }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SongViewModel @Inject constructor(db: PwsDatabase) : ViewModel() {
  private val songDao = db.songDao()
  private val songNumberDao = db.songNumberDao()
  private val favoriteDao = db.favoriteDao()
  private val historyDao = db.historyDao()
  private val bookDao = db.bookDao()
  private val songTagDao = db.songTagDao()
  private val tagDao = db.tagDao()
  private val songReferenceDao = db.songReferenceDao()

  private val _songNumberId = MutableStateFlow<SongNumberId?>(null)
  val songNumberId: StateFlow<SongNumberId?> get() = _songNumberId.asStateFlow()

  fun setSongNumberId(songNumberId: SongNumberId?) {
    if (_songNumberId.value != songNumberId) {
      _songNumberId.value = songNumberId
      Timber.d("song number id is changed: $songNumberId")
    }
  }

  val song: StateFlow<SongInfo?> = _songNumberId.filterNotNull()
    .flatMapLatest { songNumberId ->
      val songFlow = songDao.getByIdFlow(songNumberId.songId).filterNotNull()
      val songNumberFlow = songNumberDao.getByIdFlow(songNumberId).filterNotNull()
      val bookFlow = bookDao.getByIdFlow(songNumberId.bookId).filterNotNull()
      combine(songFlow, songNumberFlow, bookFlow) { song, songNumber, book ->
        Triple(song, songNumber, book)
      }
    }.flatMapLatest { (song, songNumber, book) ->
      val tagsFlow = tagDao.getBySongIdFlow(songNumber.songId)
      val favoriteFlow = favoriteDao.getByIdFlow(songNumber.id)
      val songReferencesFlow = songReferenceDao.getActiveReferredSongsBySongIdFlow(songNumber.songId).mapLatest { it.distinctBy { r -> r.song.id } }
      val allBookNumbersFlow = songNumberDao.getByBookIdFlow(songNumber.bookId)
      combine(tagsFlow, favoriteFlow, songReferencesFlow, allBookNumbersFlow) { tags, favorite, songReferences, allBookNumbers ->
        SongInfo(
          song = song,
          book = book,
          songNumber = songNumber,
          tags = tags,
          favorite = favorite,
          references = songReferences,
          allBookNumbers = allBookNumbers
        )
      }
    }
    .distinctUntilChanged()
    .onEach { s ->
      Timber.d(
        "fetched new data by song number ${s.songNumberId}: song #${s.song.id}, book ${s.book.id}, " +
          "all book song numbers (count = ${s.allBookNumbers.size}), tags (count = ${s.tags.size}), references (count = ${s.references.size})"
      )
    }
    .catch { e -> Timber.e(e, "error fetching song data by song number id ${songNumberId.value}: ${e.message}") }
    .stateIn(viewModelScope, SharingStarted.Lazily, null)

  val isFavorite = song.mapLatest { it?.isFavorite == true }.distinctUntilChanged()
  val number = song.mapLatest { it?.songNumber?.number }.distinctUntilChanged()
  val tags: Flow<List<TagEntity>?> = song.mapLatest { it?.tags }.distinctUntilChanged()
  val allBookNumbers: Flow<List<SongNumberEntity>?> = song.mapLatest { it?.allBookNumbers }.distinctUntilChanged()
  val references: Flow<List<SongReferenceDetailsEntity>?> = song.mapLatest { it?.references }.distinctUntilChanged()

  suspend fun update(updateFn: suspend (existing: SongEntity) -> SongEntity) {
    val nextSong = song.value?.song?.let { song ->
      val nextSong = updateFn(song)
      require(song.id == nextSong.id) { "unable to change song id" }
      nextSong.copy(version = song.version.nextMinor(), edited = true)
    }
    if (nextSong != null) {
      songDao.update(nextSong)
      Timber.i("song #${nextSong.id} updated")
    }
  }

  suspend fun setTags(newSongTags: List<TagEntity>) {
    val songNumberId = songNumberId.value ?: return
    val existing = songTagDao.getBySongId(songNumberId.songId).toSet()
    val target = newSongTags.map { SongTagEntity(songNumberId.songId, it.id) }.toSet()
    val tagsToRemove = existing - target
    val tagsToAdd = target - existing
    songTagDao.delete(tagsToRemove.toList())
    songTagDao.insert(tagsToAdd.toList())
    Timber.d(
      "tags updated for song number id $songNumberId: " +
        "removed: ${tagsToRemove.joinToString(",") { it.tagId.toString() }}; " +
        "added: ${tagsToAdd.joinToString(",") { it.tagId.toString() }}"
    )
  }

  suspend fun toggleFavorite() {
    _songNumberId.value?.let { songNumberId ->
      favoriteDao.toggleFavorite(songNumberId)
      Timber.d("toggled favorite song: song number id = $songNumberId")
    }
  }

  suspend fun addToHistory() {
    runCatching {
      song.value?.let { song ->
        val lastHistoryItem = historyDao.getLast().firstOrNull()
        if (lastHistoryItem == null || lastHistoryItem.songNumberId != song.songNumberId) {
          Timber.d("song #${song.song.id} (book ${song.book.id}, number ${song.songNumber.number}) added to history.")
          historyDao.insert(HistoryEntity(songNumberId = song.songNumberId))
        } else {
          Timber.d("song #${song.song.id} (book ${song.book.id}, number ${song.songNumber.number}) already last in history.")
        }
      }
    }.onFailure { e ->
      // ignore possible exception because addToHistory is not critical functionality
      Timber.e(e, "error inserting song #${song.value?.song?.id} to history: ${e.message}")
    }
  }
}