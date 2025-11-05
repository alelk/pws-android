package io.github.alelk.pws.features.di

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.alelk.pws.domain.book.repository.BookRepository
import io.github.alelk.pws.domain.song.repository.SongRepository

// Composition locals as lightweight DI until Hilt integration.
val LocalBookRepository = staticCompositionLocalOf<BookRepository> { error("BookRepository not provided") }
val LocalSongRepository = staticCompositionLocalOf<SongRepository> { error("SongRepository not provided") }

