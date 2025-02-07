/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.alelk.pws.android.app.fragment

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
import io.github.alelk.pws.android.app.activity.SongActivity
import io.github.alelk.pws.android.app.adapter.SearchRecyclerViewAdapter
import io.github.alelk.pws.android.app.model.SearchSongViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
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
    val adapter = SearchRecyclerViewAdapter { songNumberId: Long ->
      val intent = Intent(requireContext(), SongActivity::class.java)
      intent.putExtra(SongActivity.KEY_SONG_NUMBER_ID, songNumberId)
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