package io.github.alelk.pws.features.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.features.book.songs.BookSongsScreen
import io.github.alelk.pws.features.book.songs.BookSongsUiState
import io.github.alelk.pws.features.books.BooksScreen
import io.github.alelk.pws.features.books.BooksUiState
import io.github.alelk.pws.features.song.detail.SongDetailScreen
import io.github.alelk.pws.features.song.detail.SongDetailUiState

@Composable
fun AppNavGraph(
  modifier: Modifier = Modifier,
  booksState: BooksUiState,
  onBookClick: (BookId) -> Unit,
  onSearch: (String) -> Unit,
  bookSongsStateProvider: (BookId) -> BookSongsUiState,
  onSongClick: (BookId, SongId) -> Unit,
  songDetailStateProvider: (SongId) -> SongDetailUiState,
  onBack: () -> Unit,
) {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = NavRoutes.Books, modifier = modifier) {
    composable(NavRoutes.Books) {
      BooksScreen(
        state = booksState,
        onBookClick = { b ->
          onBookClick(b.id)
          navController.navigate("${NavRoutes.BookSongs}/${b.id}")
        },
        onSearchChange = onSearch
      )
    }
    composable("${NavRoutes.BookSongs}/{${NavRoutes.ArgBookId}}", arguments = listOf(
      navArgument(NavRoutes.ArgBookId) { type = NavType.StringType }
    )) { entry ->
      val bookId = BookId.parse(entry.arguments?.getString(NavRoutes.ArgBookId)!!) // safe
      val state = bookSongsStateProvider(bookId)
      BookSongsScreen(
        state = state,
        onSongClick = { song -> navController.navigate("${NavRoutes.SongDetail}/${song.id.value}") },
        onBack = { navController.popBackStack() }
      )
    }
    composable("${NavRoutes.SongDetail}/{${NavRoutes.ArgSongId}}", arguments = listOf(
      navArgument(NavRoutes.ArgSongId) { type = NavType.LongType }
    )) { entry ->
      val songId = SongId(entry.arguments?.getLong(NavRoutes.ArgSongId)!!)
      SongDetailScreen(state = songDetailStateProvider(songId), onBack = { navController.popBackStack() })
    }
  }
}

