package io.github.alelk.pws.android.app.feature.songs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

/**
 * Song Header Fragment
 *
 * Created by Alex Elkin on 11.05.2016.
 */
@AndroidEntryPoint
class SongHeaderFragment : Fragment() {
  private val songViewModel: SongViewModel by activityViewModels()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_song_header, container, false)

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        songViewModel.song.filterNotNull().mapLatest { it.song to it.book }.distinctUntilChanged().collectLatest { (song, book) ->
          view.findViewById<TextView>(R.id.txt_song_name).text = song.name
          view.findViewById<TextView>(R.id.txt_book_name).text = book.displayName
          view.findViewById<TextView>(R.id.txt_bible_ref).text = song.bibleRef?.text ?: ""
        }
      }
    }
  }
}