package io.github.alelk.pws.android.app.feature.books

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.feature.songs.SongActivity
import io.github.alelk.pws.android.app.feature.books.BooksRecyclerViewAdapter
import io.github.alelk.pws.android.app.feature.books.BooksViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Books Fragment
 *
 * Created by Mykhailo Dmytriakha on 05.03.2024
 */
@AndroidEntryPoint
class BooksFragment @Inject constructor() : Fragment() {

  private lateinit var booksAdapter: BooksRecyclerViewAdapter
  private val bookViewModel: BooksViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_books, container, false)
    val recyclerView = view.findViewById<RecyclerView>(R.id.rv_books)
    val layoutManager = LinearLayoutManager(requireContext())
    recyclerView.layoutManager = layoutManager
    booksAdapter = BooksRecyclerViewAdapter { book ->
      val intent = Intent(requireActivity(), SongActivity::class.java)
      book.firstSongNumberId?.let {
        intent.putExtra(
          SongActivity.KEY_SONG_NUMBER_ID,
          it.toString()
        )
      }
      startActivity(intent)
    }
    recyclerView.adapter = booksAdapter
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        bookViewModel.allActiveBooks.filterNotNull().collectLatest { books ->
          booksAdapter.submitList(books)
        }
      }
    }
    return view
  }
}