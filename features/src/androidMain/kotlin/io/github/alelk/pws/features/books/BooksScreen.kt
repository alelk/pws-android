package io.github.alelk.pws.features.books

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongNumberId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
  state: BooksUiState,
  onBookClick: (BookSummary) -> Unit,
  onSearchChange: (String) -> Unit,
  onToggleLocale: (Locale) -> Unit = {},
) {
  Scaffold(
    topBar = { TopAppBar(title = { Text("Сборники песен") }) }
  ) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
      SearchBar(state, onSearchChange)
      LocaleFilters(state, onToggleLocale)
      when {
        state.isLoading -> LoadingState()
        state.errorMessage != null -> ErrorState(state.errorMessage)
        state.books.isEmpty() -> EmptyState()
        else -> BooksList(state.books, onBookClick)
      }
    }
  }
}

@Composable private fun SearchBar(state: BooksUiState, onSearchChange: (String) -> Unit) {
  OutlinedTextField(
    value = state.search,
    onValueChange = onSearchChange,
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    label = { Text("Поиск") },
    singleLine = true
  )
}

@Composable private fun LocaleFilters(state: BooksUiState, onToggle: (Locale) -> Unit) {
  Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf(Locale.RU, Locale.UK).forEach { loc ->
      FilterChip(
        selected = state.filterLocale == loc,
        onClick = { onToggle(loc) },
        label = { Text(loc.value) }
      )
    }
  }
}

@Composable private fun LoadingState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
@Composable private fun ErrorState(msg: String) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ошибка: $msg") }
@Composable private fun EmptyState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Нет сборников") }

@Composable private fun BooksList(books: List<BookSummary>, onBookClick: (BookSummary) -> Unit) {
  LazyColumn(Modifier.fillMaxSize()) {
    items(books, key = { it.id.toString() }) { book ->
      BookRow(book = book, onClick = { onBookClick(book) })
    }
    item { Spacer(Modifier.height(32.dp)) }
  }
}

@Composable
private fun BookRow(book: BookSummary, onClick: () -> Unit) {
  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .clickable(onClick = onClick)
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(book.displayName.value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Spacer(Modifier.height(4.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AssistChip(onClick = {}, label = { Text("${book.countSongs} песен") })
        if (!book.enabled) AssistChip(onClick = {}, label = { Text("Выключен") }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.errorContainer))
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
private fun BooksScreenPreview() {
  val sample = List(5) { i ->
    BookSummary(
      id = BookId.parse("book$i"),
      version = Version(1, 0),
      locale = if (i % 2 == 0) Locale.RU else Locale.UK,
      name = NonEmptyString("Name $i"),
      displayShortName = NonEmptyString("S$i"),
      displayName = NonEmptyString("Сборник $i"),
      countSongs = 10 * (i + 1),
      firstSongNumberId = SongNumberId.parse("book$i/1"),
      enabled = i % 2 == 0,
      priority = i
    )
  }
  BooksScreen(state = BooksUiState(books = sample, isLoading = false), onBookClick = {}, onSearchChange = {}, onToggleLocale = {})
}
