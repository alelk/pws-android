package io.github.alelk.pws.features.song.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.song.lyric.Chorus
import io.github.alelk.pws.domain.song.lyric.Lyric
import io.github.alelk.pws.domain.song.lyric.Verse
import io.github.alelk.pws.domain.song.model.SongDetail
import io.github.alelk.pws.domain.tonality.Tonality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
  state: SongDetailUiState,
  onBack: () -> Unit,
) {
  Scaffold(topBar = {
    TopAppBar(
      title = { Text(state.song?.name?.value ?: "Песня") },
      navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") } }
    )
  }) { innerPadding ->
    if (state.isLoading) {
      Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
      state.song?.let { song ->
        SongDetailContent(song, Modifier.padding(innerPadding).fillMaxSize())
      } ?: Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) { Text("Не найдено") }
    }
  }
}

@Composable
private fun SongDetailContent(song: SongDetail, modifier: Modifier = Modifier) {
  val scroll = rememberScrollState()
  Column(
    modifier = modifier.verticalScroll(scroll).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    SongMeta(song)
    Divider()
    SongLyric(song)
  }
}

@Composable
private fun SongMeta(song: SongDetail) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    song.author?.let { Text("Автор: ${it.name}") }
    song.composer?.let { Text("Композитор: ${it.name}") }
    song.translator?.let { Text("Перевод: ${it.name}") }
    song.tonalities?.takeIf { it.isNotEmpty() }?.let { list ->
      Text("Тональности: ${list.joinToString { it.identifier }}")
    }
  }
}

@Composable
private fun SongLyric(song: SongDetail) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    song.lyric.forEach { part ->
      Text(
        part.text.trim(),
        fontWeight = if (part is Chorus) FontWeight.SemiBold else FontWeight.Normal
      )
    }
  }
}

@Preview(showSystemUi = true)
@Composable
private fun SongDetailScreenPreview() {
  val lyric = Lyric(listOf(
    Verse(numbers = setOf(1), text = "Куплет 1\nСлова песни..."),
    Chorus(numbers = setOf(1), text = "Припев\nСлова припева..."),
    Verse(numbers = setOf(2), text = "Куплет 2\nПродолжение текста...")
  ))
  val song = SongDetail(
    id = SongId(1),
    version = Version(1, 0),
    locale = Locale.RU,
    name = NonEmptyString("Пример песни"),
    lyric = lyric,
    author = Person(name = "Автор"),
    translator = null,
    composer = Person(name = "Композитор"),
    tonalities = listOf(Tonality.C_MAJOR, Tonality.G_MAJOR),
    year = null,
    bibleRef = null,
    edited = true
  )
  SongDetailScreen(state = SongDetailUiState(song = song, isLoading = false), onBack = {})
}
