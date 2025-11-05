package io.github.alelk.pws.features.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.features.book.songs.BookSongsUiState
import io.github.alelk.pws.features.books.BooksIntent
import io.github.alelk.pws.features.books.BooksScreen
import io.github.alelk.pws.features.books.BooksUiState
import io.github.alelk.pws.features.books.BooksViewModel
import io.github.alelk.pws.features.di.LocalBookRepository
import io.github.alelk.pws.features.di.LocalSongRepository
import io.github.alelk.pws.features.navigation.AppNavGraph
import io.github.alelk.pws.features.preview.FakeBookRepository
import io.github.alelk.pws.features.preview.FakeSongRepository
import io.github.alelk.pws.features.song.detail.SongDetailUiState
import io.github.alelk.pws.features.song.detail.SongDetailViewModel
import io.github.alelk.pws.features.usecase.ObserveBooksUseCaseTemp
import io.github.alelk.pws.features.usecase.ObserveSongsInBookUseCaseTemp
import androidx.compose.runtime.collectAsState

/** Root Composable hosting navigation and DI (temporary). */
@Composable
fun PwsFeaturesApp(modifier: Modifier = Modifier) {
  // In real app these repositories will be injected via Hilt at Activity level.
  val bookRepo = remember { FakeBookRepository() }
  val songRepo = remember { FakeSongRepository() }

  CompositionLocalProvider(LocalBookRepository provides bookRepo, LocalSongRepository provides songRepo) {
    val booksViewModel = remember { BooksViewModel(bookRepo) }
    val observeBooks = remember { ObserveBooksUseCaseTemp(bookRepo) }
    val observeSongsInBook = remember { ObserveSongsInBookUseCaseTemp(songRepo) }

    AppNavGraph(
      modifier = modifier,
      booksState = booksViewModel.uiState.collectAsState().value, // snapshot (for preview); a real implementation would collect state as State
      onBookClick = { /* navigation handled inside AppNavGraph */ },
      onSearch = { booksViewModel.dispatch(BooksIntent.Search(it)) },
      bookSongsStateProvider = { bookId: BookId ->
        BookSongsUiState(songs = observeSongsInBook(bookId = bookId)./* TODO collectAsState */ let { emptyList() }, isLoading = false)
      },
      onSongClick = { _, _ -> },
      songDetailStateProvider = { songId: SongId -> SongDetailUiState(song = null, isLoading = true) },
      onBack = { /* navController pop inside graph */ }
    )
  }
}

