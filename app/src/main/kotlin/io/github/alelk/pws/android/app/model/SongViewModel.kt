package io.github.alelk.pws.android.app.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.entity.SongSongReferenceDetailsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alelk.pws.database.entity.BookEntity
import io.github.alelk.pws.database.entity.FavoriteEntity
import io.github.alelk.pws.database.entity.HistoryEntity
import io.github.alelk.pws.database.entity.SongEntity
import io.github.alelk.pws.database.entity.SongNumberEntity
import io.github.alelk.pws.database.entity.SongNumberTagEntity
import io.github.alelk.pws.database.entity.TagEntity
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
import java.util.Date
import javax.inject.Inject

data class SongInfo(
  val song: SongEntity,
  val songNumber: SongNumberEntity,
  val tags: List<TagEntity>,
  val favorite: FavoriteEntity?,
  val book: BookEntity,
  val references: List<SongSongReferenceDetailsEntity>,
  val allBookNumbers: List<SongNumberEntity>,
) {
  val isFavorite: Boolean get() = favorite != null
  val songNumberId: Long get() = checkNotNull(songNumber.id) { "song number id cannot be null" }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SongViewModel @Inject constructor(db: PwsDatabase) : ViewModel() {
  private val songDao = db.songDao()
  private val songNumberDao = db.songNumberDao()
  private val favoriteDao = db.favoriteDao()
  private val historyDao = db.historyDao()
  private val bookDao = db.bookDao()
  private val songNumberTagDao = db.songNumberTagDao()
  private val songReferenceDao = db.songSongReferenceDao()

  private val _songNumberId = MutableStateFlow<Long?>(null)
  val songNumberId: StateFlow<Long?> get() = _songNumberId.asStateFlow()

  fun setSongNumberId(songNumberId: Long?) {
    if (_songNumberId.value != songNumberId) {
      _songNumberId.value = songNumberId
      Timber.d("song number id is changed: $songNumberId")
    }
  }

  val song: StateFlow<SongInfo?> = _songNumberId.filterNotNull()
    .flatMapLatest { songNumberId ->
      val songFlow = songNumberDao.getSongOfBookByIdFlow(songNumberId)
      val bookNumbersFlow = bookDao.getBookSongNumbersBySongNumberIdFlow(songNumberId)
      val tagsFlow = songNumberTagDao.getTagsBySongNumberIdFlow(songNumberId)
      val referencesFlow = songReferenceDao.getBySongNumberIdFlow(songNumberId)

      combine(songFlow, bookNumbersFlow, tagsFlow, referencesFlow) { songOfBook, bookNumbers, tags, references ->
        SongInfo(
          song = songOfBook.song,
          book = songOfBook.book,
          songNumber = songOfBook.songNumber,
          tags = tags,
          favorite = songOfBook.favorite,
          allBookNumbers = bookNumbers,
          references = references
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
  val references: Flow<List<SongSongReferenceDetailsEntity>?> = song.mapLatest { it?.references?.distinctBy { r -> r.refSongId } }.distinctUntilChanged()

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
    val existing = songNumberTagDao.getBySongNumberId(songNumberId).toSet()
    val target = newSongTags.map { SongNumberTagEntity(songNumberId, it.id) }.toSet()
    val tagsToRemove = existing - target
    val tagsToAdd = target - existing
    songNumberTagDao.delete(tagsToRemove.toList())
    songNumberTagDao.insert(tagsToAdd.toList())
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
    song.value?.let { song ->
      val lastHistoryItem = historyDao.getLast().firstOrNull()
      if (lastHistoryItem == null || lastHistoryItem.songNumberId != song.songNumberId) {
        Timber.d("song #${song.song.id} (book ${song.book.externalId}, number ${song.songNumber.number}) added to history.")
        historyDao.insert(HistoryEntity(songNumberId = song.songNumberId, accessTimestamp = Date()))
      } else {
        Timber.d("song #${song.song.id} (book ${song.book.externalId}, number ${song.songNumber.number}) already last in history.")
      }
    }
  }
}