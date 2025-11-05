package io.github.alelk.pws.features.book.songs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.song.model.SongSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSongsScreen(
  state: BookSongsUiState,
  onSongClick: (SongSummary) -> Unit,
  onBack: () -> Unit,
) {
  Scaffold(topBar = {
    TopAppBar(
      title = { Text("Песни сборника") },
      navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") }
      }
    )
  }) { innerPadding ->
    if (state.isLoading) {
      Box(Modifier.padding(innerPadding).fillMaxSize()) { CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center)) }
    } else {
      LazyColumn(Modifier.padding(innerPadding).fillMaxSize()) {
        items(state.songs) { item ->
          val (number, song) = item
          SongRow(number = number, song = song, onClick = { onSongClick(song) })
        }
      }
    }
  }
}

@Composable
private fun SongRow(number: Int, song: SongSummary, onClick: () -> Unit) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp)
      .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.small,
    tonalElevation = if (song.edited) 2.dp else 0.dp
  ) {
    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(number.toString(), style = MaterialTheme.typography.labelLarge)
      Column(Modifier.weight(1f)) {
        Text(song.name.value, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (song.edited) Text("Редактирована", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
private fun BookSongsScreenPreview() {
  val songs = List(15) { i -> (i + 1) to SongSummary(
    id = SongId(i.toLong()),
    version = Version(1, 0),
    locale = Locale.RU,
    name = NonEmptyString("Песня номер $i"),
    edited = i % 3 == 0
  ) }
  BookSongsScreen(state = BookSongsUiState(songs = songs, isLoading = false), onSongClick = {}, onBack = {})
}
