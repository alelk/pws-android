package io.github.alelk.pws.android.app.feature.books

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.feature.songs.SongActivity
import io.github.alelk.pws.database.book.BookWithSongNumbersEntity
import javax.inject.Inject

@AndroidEntryPoint
class BooksFragment @Inject constructor() : Fragment() {
  private val bookViewModel: BooksViewModel by viewModels()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    ComposeView(requireContext())
      .apply {
        setContent {
          MaterialTheme { // TODO replace with app theme bridge
            BooksScreen(
              viewModel = bookViewModel,
              onBookClick = { book ->
                val intent = Intent(requireActivity(), SongActivity::class.java)
                book.songNumbers.minByOrNull { it.number }?.id?.let { intent.putExtra(SongActivity.KEY_SONG_NUMBER_ID, it.toString()) }
                startActivity(intent)
              }
            )
          }
        }
      }
}

@Composable
private fun BooksScreen(viewModel: BooksViewModel, onBookClick: (BookWithSongNumbersEntity) -> Unit) {
  val books by viewModel.allActiveBooks.collectAsState(initial = emptyList())
  BooksList(books = books, onBookClick = onBookClick)
}

@Composable
private fun BooksList(books: List<BookWithSongNumbersEntity>, onBookClick: (BookWithSongNumbersEntity) -> Unit) {
  val horizontalPadding = 10.dp
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(start = horizontalPadding, end = horizontalPadding)
  ) {
    items(books, key = { it.book.id.toString() }) { book ->
      BookItem(book = book, onClick = { onBookClick(book) })
    }
  }
}

@Composable
private fun BookItem(book: BookWithSongNumbersEntity, onClick: () -> Unit) {
  val minNumberWidth = dimensionResource(id = R.dimen.txt_min_width_song_number)
  val numberHorizontalPadding = dimensionResource(id = R.dimen.txt_padding_horizontal_card)
  val verticalMargin = 2.dp
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = verticalMargin, bottom = verticalMargin)
      .clickable(onClick = onClick),
    backgroundColor = MaterialTheme.colors.surface // TODO map to cardBackgroundColor attr
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .widthIn(min = minNumberWidth)
          .padding(horizontal = numberHorizontalPadding)
          .height(IntrinsicSize.Min),
        contentAlignment = Alignment.Center
      ) {
        Text(text = book.book.displayShortName, style = MaterialTheme.typography.h6, textAlign = TextAlign.Center)
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(top = 4.dp, bottom = 4.dp)
      ) {
        Text(text = book.book.displayName, style = MaterialTheme.typography.h6, modifier = Modifier.padding(bottom = 6.dp))
        Text(text = book.book.name, style = MaterialTheme.typography.body1)
      }
    }
  }
}

// Preview placeholders (optional, rely on design-time tooling)
// @Preview
// @Composable
// private fun PreviewBookItem() { /* Provide sample data */ }
