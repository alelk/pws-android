package io.github.alelk.pws.android.app.feature.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.feature.songs.SongActivity
import io.github.alelk.pws.android.app.feature.search.SearchRecyclerViewAdapter
import io.github.alelk.pws.android.app.feature.search.SearchSongViewModel
import io.github.alelk.pws.domain.core.ids.SongNumberId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Search Result Fragment
 *
 * Created by Alex Elkin on 23.05.2016.
 */
@AndroidEntryPoint
class SearchResultsFragment : Fragment() {

  private val searchViewModel: SearchSongViewModel by activityViewModels()

  override fun onAttach(context: Context) {
    super.onAttach(context)
    searchViewModel.setSearchQuery(requireArguments().getString(KEY_QUERY))
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    val v = inflater.inflate(R.layout.fragment_search_results, null)
    val recyclerView = v.findViewById<RecyclerView>(R.id.rv_search_results)
    val layoutSearchProgress = v.findViewById<View>(R.id.layout_search_progress)

    recyclerView.layoutManager = LinearLayoutManager(requireContext())
    val adapter = SearchRecyclerViewAdapter { songNumberId: SongNumberId ->
        val intent = Intent(requireContext(), SongActivity::class.java)
        intent.putExtra(SongActivity.Companion.KEY_SONG_NUMBER_ID, songNumberId.toString())
        startActivity(intent)
    }
    recyclerView.adapter = adapter

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        searchViewModel.searchResults.collectLatest { searchResults ->
          adapter.submitList(searchResults)
          layoutSearchProgress?.visibility = View.GONE
        }
      }
    }
    return v
  }

  companion object {
    const val KEY_QUERY = "com.alelk.pws.pwapp.query"

    fun newInstance(query: String?): SearchResultsFragment {
      return SearchResultsFragment().apply {
        arguments = Bundle().apply {
          putString(KEY_QUERY, query)
        }
      }
    }
  }
}