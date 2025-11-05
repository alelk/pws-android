package io.github.alelk.pws.features.preview

import io.github.alelk.pws.domain.book.model.BookDetail
import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.book.query.BookSort
import io.github.alelk.pws.domain.book.repository.BookRepository
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.domain.song.model.SongDetail
import io.github.alelk.pws.domain.song.model.SongSummary
import io.github.alelk.pws.domain.song.lyric.Chorus
import io.github.alelk.pws.domain.song.lyric.Lyric
import io.github.alelk.pws.domain.song.lyric.Verse
import io.github.alelk.pws.domain.song.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBookRepository : BookRepository {
  private val booksFlow = MutableStateFlow(generateBooks())

  override fun observe(id: BookId): Flow<BookDetail?> = booksFlow.map {
    it.firstOrNull { b -> b.id == id }?.let { // map to detail placeholder
      BookDetail(
        id = it.id,
        version = it.version,
        locale = it.locale,
        name = it.name,
        displayShortName = it.displayShortName,
        displayName = it.displayName,
        countSongs = it.countSongs,
        firstSongNumberId = it.firstSongNumberId,
        enabled = it.enabled,
        priority = it.priority
      )
    }
  }

  override fun observeMany(
    query: BookQuery,
    sort: BookSort
  ): Flow<List<BookSummary>> {
    TODO("Not yet implemented")
  }

  override suspend fun get(id: BookId): BookDetail? =
    booksFlow.value.firstOrNull { b -> b.id == id }?.let { // map to detail placeholder
      BookDetail(
        id = it.id,
        version = it.version,
        locale = it.locale,
        name = it.name,
        displayShortName = it.displayShortName,
        displayName = it.displayName,
        countSongs = it.countSongs,
        firstSongNumberId = it.firstSongNumberId,
        enabled = it.enabled,
        priority = it.priority
      )
    }

  private fun generateBooks(): List<BookSummary> = List(8) { i ->
    BookSummary(
      id = BookId.parse("book$i"),
      version = Version(1, i),
      locale = if (i % 2 == 0) Locale.RU else Locale.UK,
      name = NonEmptyString("BookName $i"),
      displayShortName = NonEmptyString("B$i"),
      displayName = NonEmptyString("Сборник $i"),
      countSongs = 12 + i,
      firstSongNumberId = SongNumberId.parse("book$i/1"),
      enabled = i % 3 != 0,
      priority = i
    )
  }
}

class FakeSongRepository : SongRepository {
  private val songsByBook = MutableStateFlow(generateSongs())
  override fun observe(id: SongId): Flow<SongDetail?> = songsByBook.map { map -> map.values.flatten().firstOrNull { it.id == id }?.toDetail() }
  override fun observeAllInBook(bookId: BookId): Flow<Map<Int, SongSummary>> = songsByBook.map { map ->
    map[bookId]!!.associateBy({ it.id.value.toInt() }, { it })
  }

  private fun generateSongs(): Map<BookId, List<SongSummary>> = (0 until 8).associate { bi ->
    val bookId = BookId.parse("book$bi")
    bookId to List(20) { si ->
      SongSummary(
        id = SongId(si.toLong()),
        version = Version(1, 0),
        locale = if (si % 2 == 0) Locale.RU else Locale.UK,
        name = NonEmptyString("Песня $si"),
        edited = si % 5 == 0
      )
    }
  }

  private fun SongSummary.toDetail(): SongDetail = SongDetail(
    id = id,
    version = version,
    locale = locale,
    name = name,
    lyric = Lyric(
      listOf(
        Verse(numbers = setOf(1), text = "Куплет 1\nТекст..."),
        Chorus(numbers = setOf(1), text = "Припев\nТекст припева"),
        Verse(numbers = setOf(2), text = "Куплет 2\nПродолжение...")
      )
    ),
    author = null,
    translator = null,
    composer = null,
    tonalities = listOf(),
    year = null,
    bibleRef = null,
    edited = edited
  )
}


