package io.github.alelk.pws.contentdelivery

import io.github.alelk.pws.domain.core.Color
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.color
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.TagId
import io.github.alelk.pws.domain.core.ids.bookId
import io.github.alelk.pws.domain.core.ids.songId
import io.github.alelk.pws.domain.core.ids.tagId
import io.github.alelk.pws.domain.core.locale
import io.github.alelk.pws.domain.core.version
import io.github.alelk.pws.portable.model.Book
import io.github.alelk.pws.portable.model.BookBundle
import io.github.alelk.pws.portable.model.Song
import io.github.alelk.pws.portable.model.SongNumber
import io.github.alelk.pws.portable.model.SongReference
import io.github.alelk.pws.portable.model.Tag
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.string
import kotlinx.datetime.LocalDateTime

/**
 * Kotest property generators for the portable bundle models, matching the project's
 * `Arb.Companion.xxx(...)` fixture style (cf. `BookEntityArb`). Every field is an [Arb] parameter
 * so tests can pin the fields under test (`Arb.constant(...)`) and randomise the rest.
 */

fun Arb.Companion.portableSongNumber(
  bookId: Arb<BookId> = Arb.bookId().removeEdgecases(),
  number: Arb<Int> = Arb.int(1..5000),
): Arb<SongNumber> = arbitrary { SongNumber(bookId.bind(), number.bind()) }

fun Arb.Companion.portableBook(
  id: Arb<BookId> = Arb.bookId().removeEdgecases(),
  version: Arb<Version> = Arb.version(),
  locales: Arb<List<Locale>> = Arb.list(Arb.locale(), 1..2).map { it.distinct() },
  name: Arb<String> = Arb.string(5..40, Codepoint.az()),
  displayShortName: Arb<String> = Arb.string(2..10, Codepoint.az()),
  displayName: Arb<String> = Arb.string(5..40, Codepoint.az()),
  priority: Arb<Int> = Arb.int(0..20),
): Arb<Book> = arbitrary {
  Book(
    id = id.bind(),
    version = version.bind(),
    locales = locales.bind(),
    name = name.bind(),
    displayShortName = displayShortName.bind(),
    displayName = displayName.bind(),
    priority = priority.bind(),
  )
}

fun Arb.Companion.portableSong(
  id: Arb<SongId> = Arb.songId(),
  number: Arb<SongNumber> = Arb.portableSongNumber(),
  version: Arb<Version> = Arb.version(),
  locale: Arb<Locale> = Arb.locale(),
  name: Arb<String> = Arb.string(5..40, Codepoint.az()),
  lyric: Arb<String> = Arb.string(5..120, Codepoint.az()),
): Arb<Song> = arbitrary {
  Song(
    number = number.bind(),
    id = id.bind(),
    version = version.bind(),
    locale = locale.bind(),
    name = name.bind(),
    lyric = lyric.bind(),
  )
}

fun Arb.Companion.songReference(
  songId: Arb<SongId> = Arb.songId(),
  refSongId: Arb<SongId> = Arb.songId(),
  reason: Arb<String> = Arb.constant("variation"),
  volume: Arb<Int> = Arb.int(1..100),
): Arb<SongReference> = arbitrary {
  SongReference(songId = songId.bind(), refSongId = refSongId.bind(), reason = reason.bind(), volume = volume.bind())
}

fun Arb.Companion.portableTag(
  id: Arb<TagId> = Arb.tagId(),
  name: Arb<String> = Arb.string(3..20, Codepoint.az()),
  color: Arb<Color> = Arb.color(),
  songs: Arb<Set<SongNumber>> = Arb.list(Arb.portableSongNumber(), 0..3).map { it.toSet() },
): Arb<Tag> = arbitrary {
  Tag(id = id.bind(), name = name.bind(), color = color.bind(), songs = songs.bind())
}

fun Arb.Companion.bookBundle(
  book: Arb<Book> = Arb.portableBook(),
  songs: Arb<List<Song>> = Arb.list(Arb.portableSong(), 1..5),
  songReferences: Arb<List<SongReference>?> = Arb.constant<List<SongReference>?>(null),
  tags: Arb<List<Tag>?> = Arb.constant<List<Tag>?>(null),
  createdAt: LocalDateTime = LocalDateTime(2026, 1, 1, 0, 0),
): Arb<BookBundle> = arbitrary {
  BookBundle(
    metadata = BookBundle.Metadata(createdAt = createdAt),
    book = book.bind(),
    songs = songs.bind(),
    songReferences = songReferences.bind(),
    tags = tags.bind(),
  )
}
