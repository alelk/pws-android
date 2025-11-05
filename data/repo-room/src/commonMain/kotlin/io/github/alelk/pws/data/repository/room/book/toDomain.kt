package io.github.alelk.pws.data.repository.room.book

import io.github.alelk.pws.database.book.BookDetailProjection
import io.github.alelk.pws.database.book.BookSummaryProjection
import io.github.alelk.pws.domain.book.model.BookDetail
import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.core.NonEmptyString

fun BookSummaryProjection.toDomain() =
  BookSummary(
    id = id,
    version = version,
    locale = locale,
    name = NonEmptyString(name),
    displayShortName = NonEmptyString(displayShortName),
    displayName = NonEmptyString(displayName),
    countSongs = countSongs,
    firstSongNumberId = firstSongNumberId,
    enabled = priority > 0,
    priority = priority,
  )

fun BookDetailProjection.toDomain() =
  BookDetail(
    id = id,
    version = version,
    locale = locale,
    name = NonEmptyString(name),
    displayShortName = NonEmptyString(displayShortName),
    displayName = NonEmptyString(displayName),
    releaseDate = releaseDate,
    authors = authors,
    creators = creators,
    reviewers = reviewers,
    editors = editors,
    description = description,
    preface = preface,
    countSongs = countSongs,
    firstSongNumberId = firstSongNumberId,
    enabled = priority > 0,
    priority = priority,
  )